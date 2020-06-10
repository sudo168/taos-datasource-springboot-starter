package net.ewant.taos.exception;

public class TaosExhaustedPoolException extends TaosException {
    private static final long serialVersionUID = -2868148654322598483L;

    public TaosExhaustedPoolException(String message) {
        super(message);
    }

    public TaosExhaustedPoolException(Throwable e) {
        super(e);
    }

    public TaosExhaustedPoolException(String message, Throwable cause) {
        super(message, cause);
    }
}
