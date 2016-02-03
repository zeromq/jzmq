package org.zeromq;

/**
 * ZeroMQ runtime exception.
 * 
 * @author Alois Belaska &lt;alois.belaska@gmail.com&gt;
 */
public class ZMQException extends RuntimeException {
    private static final long serialVersionUID = -978820750094924644L;

    private int errorCode = 0;

    public ZMQException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ZMQException(ZMQException cause) {
        super(cause.getMessage(), cause);
        this.errorCode = cause.errorCode;
    }

    /**
     * @return error code
     */
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return super.toString() + "(0x" + Integer.toHexString(errorCode) + ")";
    }
}
