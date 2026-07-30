package no.nav.ung.sak.formidling.vedtak.regler.strategy;

/**
 * Presedens for en {@link VedtaksbrevInnholdbyggerStrategy}. Avgjør hvordan resolveren kombinerer resultater
 * på tvers av strategier, uten at den enkelte strategien trenger å kjenne til de andre.
 */
public enum Presedens {

    /**
     * Overstyrer alt: hvis en slik strategi er aktuell, blir resultatet ingen brev for hele behandlingen.
     * Brukes f.eks. ved dødsfall av barn.
     */
    OVERSTYRENDE_INGEN_BREV,

    /**
     * Overstyrer normale strategier: hvis aktuell og den produserer brev, blir det det eneste brevet.
     * Maks én slik strategi kan gi brev for samme behandling (strategiene må være gjensidig utelukkende);
     * resolveren feiler dersom flere gjør krav på dette nivået.
     */
    OVERSTYRENDE_ENKELTBREV,

    /**
     * Normal, additiv strategi. Flere normale brev kan produseres samtidig.
     */
    NORMAL
}
