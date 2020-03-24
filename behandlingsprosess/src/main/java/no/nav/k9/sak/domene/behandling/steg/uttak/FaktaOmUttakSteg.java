package no.nav.k9.sak.domene.behandling.steg.uttak;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ApplicationScoped
@BehandlingStegRef(kode = "KOFAKUT")
@BehandlingTypeRef
@FagsakYtelseTypeRef
public class FaktaOmUttakSteg implements BehandlingSteg {

    private UttakRepository uttakRepository;

    protected FaktaOmUttakSteg() {
        // for proxy
    }

    @Inject
    public FaktaOmUttakSteg(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var oppgittUttak = uttakRepository.hentOppgittUttak(behandlingId);

        // TODO (FC): åpne aksjonspunkter når følgende
        // 1. sjekk mismatch mellom uttakaktiviteter og godkjente arbeidsforhold (ta utgangspunkt i godkjent for beregningsgrunnlag?)
        // 2. sjekk om flere arbeisforhold godkjent per arbeidsgiver

        // tar rå kopi av dataene i første omgang
        List<UttakAktivitetPeriode> mappedPerioder = oppgittUttak.getPerioder().stream().map(this::mapUttakAktivitetPeriode).collect(Collectors.toList());
        var fastsattUttak = new UttakAktivitet(mappedPerioder);
        uttakRepository.lagreOgFlushFastsattUttak(behandlingId, fastsattUttak);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private UttakAktivitetPeriode mapUttakAktivitetPeriode(UttakAktivitetPeriode p) {
        return new UttakAktivitetPeriode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(),
            p.getAktivitetType(), p.getArbeidsgiver(),
            p.getArbeidsforholdRef() != null ? p.getArbeidsforholdRef() : InternArbeidsforholdRef.nullRef(), // legger på null objekt her for sammenligne enklere senre
            p.getJobberNormaltPerUke(),
            p.getSkalJobbeProsent());
    }
}
