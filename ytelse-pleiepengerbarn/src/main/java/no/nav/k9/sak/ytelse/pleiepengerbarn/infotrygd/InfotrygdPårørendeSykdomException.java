package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygd;

public class InfotrygdPårørendeSykdomException extends RuntimeException {
    public InfotrygdPårørendeSykdomException(String message) {
        super(message);
    }

    public InfotrygdPårørendeSykdomException(String message, Throwable cause) {
        super(message, cause);
    }
}
