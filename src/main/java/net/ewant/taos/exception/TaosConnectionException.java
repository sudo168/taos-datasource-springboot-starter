package net.ewant.taos.exception;

public class TaosConnectionException extends TaosException {

    private static final long serialVersionUID = -152207873528802170L;

    public TaosConnectionException(String message) {
        super(message);
    }

    public TaosConnectionException(Throwable e) {
        super(e);
    }

    public TaosConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
