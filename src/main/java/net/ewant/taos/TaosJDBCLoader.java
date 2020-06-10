package net.ewant.taos;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;

public class TaosJDBCLoader {

    private static final String LIBRARY_NAME = "taos";

    public static synchronized boolean initialize() throws Exception {
        if(checkSystemLibrary()){
            return true;
        }
        return loadNativeLibrary();
    }

    private static boolean checkSystemLibrary() throws Exception{
        String libraryName = System.mapLibraryName(LIBRARY_NAME);
        Field usrPaths = ClassLoader.class.getDeclaredField("usr_paths");
        usrPaths.setAccessible(true);
        String[] usr = (String[]) usrPaths.get(ClassLoader.class);
        if(usr != null){
            for(String path : usr){
                if(new File(path, libraryName).exists()){
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean loadNativeLibrary() throws Exception{
        String packagePath = TaosJDBCLoader.class.getPackage().getName().replaceAll("\\.", "/");
        String nativeLibraryPath = String.format("/%s/native/%s", packagePath, OSInfo.getNativeLibFolderPathForCurrentOS());
        String libraryName = System.mapLibraryName(LIBRARY_NAME);
        String nativeLibraryFilePath = nativeLibraryPath + "/" + libraryName;
        boolean hasNativeLib = hasResource(nativeLibraryFilePath);
        if(hasNativeLib){
            String tmpPath = getTempDir().getAbsolutePath();
            File extractedLibFile = new File(tmpPath, libraryName);
            if(extractedLibFile.exists()){
                extractedLibFile.delete();
            }
            InputStream reader = TaosJDBCLoader.class.getResourceAsStream(nativeLibraryFilePath);
            FileOutputStream writer = new FileOutputStream(extractedLibFile);
            try {
                byte[] buffer = new byte[8192];
                int bytesRead = 0;
                while((bytesRead = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, bytesRead);
                }
                addLibraryPath(tmpPath);
                return true;
            }
            finally {
                // Delete the extracted lib file on JVM exit.
                extractedLibFile.deleteOnExit();

                if(writer != null) {
                    writer.close();
                }
                if(reader != null) {
                    reader.close();
                }
            }
        }
        return false;
    }

    private static void addLibraryPath(String tmpPath) throws NoSuchFieldException, IllegalAccessException {
        Field usrPaths = ClassLoader.class.getDeclaredField("usr_paths");
        usrPaths.setAccessible(true);
        String[] usr = (String[]) usrPaths.get(ClassLoader.class);
        String[] newUsr;
        if(usr == null || usr.length == 0){
            newUsr = new String[]{tmpPath};
        }else{
            newUsr = new String[usr.length + 1];
            for(int i=0; i<usr.length;i++){
                newUsr[i] = usr[i];
            }
            newUsr[usr.length] = tmpPath;
        }
        usrPaths.set(ClassLoader.class, newUsr);
    }

    private static File getTempDir() {
        return new File(System.getProperty("com.taosdata.tmpdir", System.getProperty("java.io.tmpdir")));
    }

    private static boolean hasResource(String path) {
        return TaosJDBCLoader.class.getResource(path) != null;
    }

    public static class OSInfo{
        private static HashMap<String, String> archMapping = new HashMap();
        public static final String X86 = "x86";
        public static final String X86_64 = "x86_64";
        public static final String IA64_32 = "ia64_32";
        public static final String IA64 = "ia64";
        public static final String PPC = "ppc";
        public static final String PPC64 = "ppc64";

        public OSInfo() {
        }

        public static void main(String[] args) {
            if (args.length >= 1) {
                if ("--os".equals(args[0])) {
                    System.out.print(getOSName());
                    return;
                }

                if ("--arch".equals(args[0])) {
                    System.out.print(getArchName());
                    return;
                }
            }

            System.out.print(getNativeLibFolderPathForCurrentOS());
        }

        public static String getNativeLibFolderPathForCurrentOS() {
            return getOSName() + "/" + getArchName();
        }

        public static String getOSName() {
            return translateOSNameToFolderName(System.getProperty("os.name"));
        }

        public static boolean isAndroid() {
            return System.getProperty("java.runtime.name", "").toLowerCase().contains("android");
        }

        static String getHardwareName() {
            try {
                Process p = Runtime.getRuntime().exec("uname -m");
                p.waitFor();
                InputStream in = p.getInputStream();

                try {
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    byte[] buf = new byte[32];

                    int readLen;
                    while((readLen = in.read(buf, 0, buf.length)) >= 0) {
                        b.write(buf, 0, readLen);
                    }

                    return b.toString();
                } finally {
                    if (in != null) {
                        in.close();
                    }

                }
            } catch (Throwable e) {
                System.err.println("Error while running uname -m: " + e.getMessage());
                return "unknown";
            }
        }

        static String resolveArmArchType() {
            if (System.getProperty("os.name").contains("Linux")) {
                String armType = getHardwareName();
                if (armType.startsWith("armv6")) {
                    return "armv6";
                }

                if (armType.startsWith("armv7")) {
                    return "armv7";
                }

                if (armType.startsWith("armv5")) {
                    return "arm";
                }

                if (armType.equals("aarch64")) {
                    return "arm64";
                }

                String abi = System.getProperty("sun.arch.abi");
                if (abi != null && abi.startsWith("gnueabihf")) {
                    return "armv7";
                }

                String javaHome = System.getProperty("java.home");

                try {
                    int exitCode = Runtime.getRuntime().exec("which readelf").waitFor();
                    if (exitCode == 0) {
                        String[] cmdarray = new String[]{"/bin/sh", "-c", "find '" + javaHome + "' -name 'libjvm.so' | head -1 | xargs readelf -A | grep 'Tag_ABI_VFP_args: VFP registers'"};
                        exitCode = Runtime.getRuntime().exec(cmdarray).waitFor();
                        if (exitCode == 0) {
                            return "armv7";
                        }
                    } else {
                        System.err.println("WARNING! readelf not found. Cannot check if running on an armhf system, armel architecture will be presumed.");
                    }
                } catch (IOException e) {
                } catch (InterruptedException e) {
                }
            }

            return "arm";
        }

        public static String getArchName() {
            String osArch = System.getProperty("os.arch");
            if (isAndroid()) {
                return "android-arm";
            } else {
                if (osArch.startsWith("arm")) {
                    osArch = resolveArmArchType();
                } else {
                    String lc = osArch.toLowerCase(Locale.US);
                    if (archMapping.containsKey(lc)) {
                        return (String)archMapping.get(lc);
                    }
                }

                return translateArchNameToFolderName(osArch);
            }
        }

        static String translateOSNameToFolderName(String osName) {
            if (osName.contains("Windows")) {
                return "Windows";
            } else if (!osName.contains("Mac") && !osName.contains("Darwin")) {
                if (osName.contains("Linux")) {
                    return "Linux";
                } else {
                    return osName.contains("AIX") ? "AIX" : osName.replaceAll("\\W", "");
                }
            } else {
                return "Mac";
            }
        }

        static String translateArchNameToFolderName(String archName) {
            return archName.replaceAll("\\W", "");
        }

        static {
            archMapping.put("x86", "x86");
            archMapping.put("i386", "x86");
            archMapping.put("i486", "x86");
            archMapping.put("i586", "x86");
            archMapping.put("i686", "x86");
            archMapping.put("pentium", "x86");
            archMapping.put("x86_64", "x86_64");
            archMapping.put("amd64", "x86_64");
            archMapping.put("em64t", "x86_64");
            archMapping.put("universal", "x86_64");
            archMapping.put("ia64", "ia64");
            archMapping.put("ia64w", "ia64");
            archMapping.put("ia64_32", "ia64_32");
            archMapping.put("ia64n", "ia64_32");
            archMapping.put("ppc", "ppc");
            archMapping.put("power", "ppc");
            archMapping.put("powerpc", "ppc");
            archMapping.put("power_pc", "ppc");
            archMapping.put("power_rs", "ppc");
            archMapping.put("ppc64", "ppc64");
            archMapping.put("power64", "ppc64");
            archMapping.put("powerpc64", "ppc64");
            archMapping.put("power_pc64", "ppc64");
            archMapping.put("power_rs64", "ppc64");
        }
    }
}
