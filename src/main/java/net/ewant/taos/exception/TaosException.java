package net.ewant.taos.exception;

public class TaosException extends RuntimeException {

    private static final long serialVersionUID = -3195075677803785405L;

    public TaosException(String message) {
        super(message);
    }

    public TaosException(Throwable e) {
        super(e);
    }

    public TaosException(String message, Throwable cause) {
        super(message, cause);
    }
}
