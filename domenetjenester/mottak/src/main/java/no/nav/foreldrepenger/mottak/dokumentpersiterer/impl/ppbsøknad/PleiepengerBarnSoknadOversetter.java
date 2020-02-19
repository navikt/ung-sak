package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.ppbsøknad;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.Fordeling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Pleietrengende;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.søknad.felles.Barn;
import no.nav.k9.søknad.felles.Bosteder;
import no.nav.k9.søknad.felles.Språk;
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class PleiepengerBarnSoknadOversetter {

    private VirksomhetTjeneste virksomhetTjeneste;
    private SøknadRepository søknadRepository;
    private MedlemskapRepository medlemskapRepository;
    private FordelingRepository fordelingRepository;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private TpsTjeneste tpsTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    PleiepengerBarnSoknadOversetter() {
        // for CDI proxy
    }

    @Inject
    public PleiepengerBarnSoknadOversetter(BehandlingRepositoryProvider repositoryProvider,
                                           VirksomhetTjeneste virksomhetTjeneste,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           FordelingRepository fordelingRepository,
                                           MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                                           TpsTjeneste tpsTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.fordelingRepository = fordelingRepository;
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.tpsTjeneste = tpsTjeneste;
    }

    //@Override
    public void persister(PleiepengerBarnSøknad soknad, Behandling behandling) {
        // TODO:
        final boolean elektroniskSøknad = false;
        final SøknadEntitet.Builder søknadBuilder = new SøknadEntitet.Builder()
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(soknad.mottattDato.toLocalDate())
            .medErEndringssøknad(false)
            .medSøknadsdato(soknad.mottattDato.toLocalDate()) // TODO: Hva er dette? Dette feltet er datoen det gjelder fra for FP-endringssøknader.
            .medSpråkkode(getSpraakValg(soknad.språk));

        // Utgår for K9-ytelsene?
        //.medBegrunnelseForSenInnsending(wrapper.getBegrunnelseForSenSoeknad())
        //.medTilleggsopplysninger(wrapper.getTilleggsopplysninger())

        final Long behandlingId = behandling.getId();

        byggMedlemskap(soknad.bosteder, behandlingId, soknad.mottattDato.toLocalDate());

        byggPleietrengende(behandling, soknad.barn);

        final Set<FordelingPeriode> perioder = mapTilPerioder(soknad);
        final var fordeling = new Fordeling(perioder);
        fordelingRepository.lagreOgFlush(behandling, fordeling);

        final SøknadEntitet søknadEntitet = søknadBuilder
            .build();
        søknadRepository.lagreOgFlush(behandling, søknadEntitet);
    }

    private void byggPleietrengende(Behandling behandling, Barn barn) {
        final var norskIdentitetsnummer = barn.norskIdentitetsnummer;
        if (norskIdentitetsnummer != null) {
            final var aktørId = tpsTjeneste.hentAktørForFnr(PersonIdent.fra(norskIdentitetsnummer.verdi));
            medisinskGrunnlagRepository.lagre(behandling, new Pleietrengende(aktørId.orElseThrow()));
        } else {
            throw new IllegalArgumentException();
        }
    }

    private Set<FordelingPeriode> mapTilPerioder(PleiepengerBarnSøknad soknad) {
        return soknad.perioder.keySet().stream()
            .map(periode -> new FordelingPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.fraOgMed, periode.tilOgMed)))
            .collect(Collectors.toSet());
    }

    private void byggMedlemskap(Bosteder bosteder, Long behandlingId, LocalDate forsendelseMottatt) {
        final MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppgittDato(forsendelseMottatt);

        // TODO: Hva skal vi ha som "oppholdNå"?
        //Boolean iNorgeVedFoedselstidspunkt = medlemskap.isINorgeVedFoedselstidspunkt();
        //oppgittTilknytningBuilder.medOppholdNå(Boolean.TRUE.equals(iNorgeVedFoedselstidspunkt));

        if (bosteder != null) {
            bosteder.perioder.forEach((periode, opphold) -> {
                // TODO: "tidligereOpphold" må fjernes fra database og domeneobjekter. Ved bruk må skjæringstidspunkt spesifikt oppgis.
                //boolean tidligereOpphold = opphold.getPeriode().getFom().isBefore(mottattDato);
                oppgittTilknytningBuilder.leggTilOpphold(new MedlemskapOppgittLandOppholdEntitet.Builder()
                    .medLand(finnLandkode(opphold.land.landkode))
                    .medPeriode(
                        Objects.requireNonNull(periode.fraOgMed),
                        Objects.requireNonNullElse(periode.tilOgMed, Tid.TIDENES_ENDE)
                    )
                    //.erTidligereOpphold(tidligereOpphold)
                    .build()
                );
            });
            medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
        }
    }

    private Språkkode getSpraakValg(Språk spraak) {
        if (spraak != null) {
            return Språkkode.fraKode(spraak.dto.toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

    private Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }
}
