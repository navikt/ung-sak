package no.nav.k9.sak.domene.uttak.repo;

public class UttaksgradRepository {

    public void lagre(Long behandlingId, UttaksgradPeriode uttaksgradPeriode) {
        // lagre uttaksgraden satt av systemet
    }

    public void lagreOverstyrteUttaksgrader(Long behandlingId) {
        // lagre den overstyrte uttaksgraden satt av saksbehandler
    }
}
