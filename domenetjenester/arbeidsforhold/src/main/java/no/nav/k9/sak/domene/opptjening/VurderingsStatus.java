package no.nav.k9.sak.domene.opptjening;

public enum VurderingsStatus {
    TIL_VURDERING,
    GODKJENT,
    UNDERKJENT,
    @Deprecated // overstyring av opptjeningsaktiviteter ble fjernet fra k9 2021-09-16
    FERDIG_VURDERT_GODKJENT,
    @Deprecated // overstyring av opptjeningsaktiviteter ble fjernet fra k9 2021-09-16
    FERDIG_VURDERT_UNDERKJENT
}
