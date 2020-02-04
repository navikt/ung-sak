package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.Innsendingsvalg;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.NamespaceRef;
import no.nav.foreldrepenger.søknad.v3.SøknadConstants;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.vedtak.felles.xml.soeknad.endringssoeknad.v3.Endringssoeknad;
import no.nav.vedtak.felles.xml.soeknad.engangsstoenad.v3.Engangsstønad;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Bruker;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Medlemskap;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.OppholdUtlandet;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Vedlegg;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Ytelse;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Innsendingstype;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Svangerskapspenger;

@NamespaceRef(SøknadConstants.NAMESPACE)
@ApplicationScoped
public class MottattDokumentOversetterSøknad implements MottattDokumentOversetter<MottattDokumentWrapperSøknad> { // NOSONAR - (essv)kan akseptere lang mapperklasse

    private PersonopplysningRepository personopplysningRepository;
    private SøknadRepository søknadRepository;
    private MedlemskapRepository medlemskapRepository;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private FagsakRepository fagsakRepository;

    MottattDokumentOversetterSøknad() {
        // for CDI proxy
    }

    @Inject
    public MottattDokumentOversetterSøknad(BehandlingRepositoryProvider repositoryProvider) {
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.behandlingRevurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    @Override
    public void trekkUtDataOgPersister(MottattDokumentWrapperSøknad wrapper, MottattDokument mottattDokument, Behandling behandling, Optional<LocalDate> gjelderFra) {
        if (wrapper.getOmYtelse() instanceof Endringssoeknad && !erEndring(mottattDokument)) { // NOSONAR - ok måte å finne riktig JAXB-type
            throw new IllegalArgumentException("Kan ikke sende inn en Endringssøknad uten å angi " + DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD + " samtidig. Fikk " + mottattDokument.getDokumentType());
        }

        if (erEndring(mottattDokument)) {
            persisterEndringssøknad(wrapper, mottattDokument, behandling, gjelderFra);
        } else {
            persisterSøknad(wrapper, mottattDokument, behandling);
        }
    }

    private SøknadEntitet.Builder kopierSøknad(Behandling behandling) {
        SøknadEntitet.Builder søknadBuilder;
        Optional<Behandling> originalBehandling = behandling.getOriginalBehandling();
        if (originalBehandling.isPresent()) {
            Long behandlingId = behandling.getId();
            Long originalBehandlingId = originalBehandling.get().getId();
            SøknadEntitet originalSøknad = søknadRepository.hentSøknad(originalBehandlingId);
            søknadBuilder = new SøknadEntitet.Builder(originalSøknad);

            MedlemskapOppgittTilknytningEntitet oppgittTilknytning = medlemskapRepository.hentMedlemskap(behandlingId).flatMap(MedlemskapAggregat::getOppgittTilknytning).orElseThrow();
            MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder(oppgittTilknytning);
            medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
        } else {
            søknadBuilder = new SøknadEntitet.Builder();
        }

        return søknadBuilder;
    }

    private void persisterEndringssøknad(MottattDokumentWrapperSøknad wrapper, MottattDokument mottattDokument, Behandling behandling, Optional<LocalDate> gjelderFra) {
        LocalDate mottattDato = mottattDokument.getMottattDato();
        boolean elektroniskSøknad = mottattDokument.getElektroniskRegistrert();

        //Kopier og oppdater søknadsfelter.
        final SøknadEntitet.Builder søknadBuilder = kopierSøknad(behandling);
        byggFelleselementerForSøknad(søknadBuilder, wrapper, elektroniskSøknad, mottattDato, gjelderFra);
        List<Behandling> henlagteBehandlingerEtterInnvilget = behandlingRevurderingRepository.finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(behandling.getFagsakId());
        if (!henlagteBehandlingerEtterInnvilget.isEmpty()) {
            søknadBuilder.medSøknadsdato(søknadRepository.hentSøknad(henlagteBehandlingerEtterInnvilget.get(0).getId()).getSøknadsdato());
        }

        if (wrapper.getOmYtelse() instanceof Endringssoeknad) { // NOSONAR
            final Endringssoeknad omYtelse = (Endringssoeknad) wrapper.getOmYtelse();
            byggYtelsesSpesifikkeFelterForEndringssøknad(omYtelse, behandling);
        }
        søknadBuilder.medErEndringssøknad(true);
        final SøknadEntitet søknad = søknadBuilder.build();

        søknadRepository.lagreOgFlush(behandling, søknad);
    }

    private void persisterSøknad(MottattDokumentWrapperSøknad wrapper, MottattDokument mottattDokument, Behandling behandling) {
        LocalDate mottattDato = mottattDokument.getMottattDato();
        boolean elektroniskSøknad = mottattDokument.getElektroniskRegistrert();
        final SøknadEntitet.Builder søknadBuilder = new SøknadEntitet.Builder();
        byggFelleselementerForSøknad(søknadBuilder, wrapper, elektroniskSøknad, mottattDato, Optional.empty());
        Long behandlingId = behandling.getId();
        AktørId aktørId = behandling.getAktørId();
        if (wrapper.getOmYtelse() != null) {
            byggMedlemskap(wrapper, behandlingId, mottattDato);
        }

        byggYtelsesSpesifikkeFelter(wrapper, behandling, søknadBuilder);
        byggOpptjeningsspesifikkeFelter(wrapper, behandlingId);

        søknadBuilder.medErEndringssøknad(false);
        final RelasjonsRolleType relasjonsRolleType = utledRolle(wrapper.getBruker(), behandlingId, aktørId);
        final SøknadEntitet søknad = søknadBuilder
            .medRelasjonsRolleType(relasjonsRolleType).build();
        søknadRepository.lagreOgFlush(behandling, søknad);
    }

    private RelasjonsRolleType utledRolle(Bruker bruker, Long behandlingId, AktørId aktørId) {
        // FIXME K9 antar Relasjonsrolletype ikke trengs normalt?  Bruk i stedet OmsorgFor vilkår
        return RelasjonsRolleType.UDEFINERT;
    }

    private boolean erEndring(MottattDokument mottattDokument) {
        return mottattDokument.getDokumentType().equals(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD);
    }

    private void byggYtelsesSpesifikkeFelterForEndringssøknad(Endringssoeknad omYtelse, Behandling behandling) {
        // FIXME K9
    }

    private void byggYtelsesSpesifikkeFelter(MottattDokumentWrapperSøknad skjemaWrapper, Behandling behandling, SøknadEntitet.Builder søknadBuilder) {
        // FIXME K9
    }

    private void byggOpptjeningsspesifikkeFelter(MottattDokumentWrapperSøknad skjemaWrapper, Long behandlingId) {
        // FIXME K9
    }

    private void byggMedlemskap(MottattDokumentWrapperSøknad skjema, Long behandlingId, LocalDate forsendelseMottatt) {
        Medlemskap medlemskap;
        Ytelse omYtelse = skjema.getOmYtelse();
        LocalDate mottattDato = skjema.getSkjema().getMottattDato();
        MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppholdNå(true).medOppgittDato(forsendelseMottatt);

        if (omYtelse instanceof Engangsstønad) { // NOSONAR - ok måte å finne riktig JAXB-type
            medlemskap = ((Engangsstønad) omYtelse).getMedlemskap();
        } else if (omYtelse instanceof Foreldrepenger) { // NOSONAR - ok måte å finne riktig JAXB-type
            medlemskap = ((Foreldrepenger) omYtelse).getMedlemskap();
        } else if (omYtelse instanceof Svangerskapspenger) { // NOSONAR - ok måte å finne riktig JAXB-type
            medlemskap = ((Svangerskapspenger) omYtelse).getMedlemskap();
        } else {
            throw new IllegalStateException("Ytelsestype er ikke støttet");
        }
        Boolean iNorgeVedFoedselstidspunkt = medlemskap.isINorgeVedFoedselstidspunkt();
        oppgittTilknytningBuilder.medOppholdNå(Boolean.TRUE.equals(iNorgeVedFoedselstidspunkt));

        Objects.requireNonNull(medlemskap, "Medlemskap må være oppgitt");

        settOppholdUtlandPerioder(medlemskap, mottattDato, oppgittTilknytningBuilder);
        settOppholdNorgePerioder(medlemskap, mottattDato, oppgittTilknytningBuilder);
        medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
    }

    private void settOppholdUtlandPerioder(Medlemskap medlemskap, LocalDate mottattDato, MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder) {
        medlemskap.getOppholdUtlandet().forEach(opphUtl -> {
            boolean tidligereOpphold = opphUtl.getPeriode().getFom().isBefore(mottattDato);
            oppgittTilknytningBuilder.leggTilOpphold(byggUtlandsopphold(opphUtl, tidligereOpphold));
        });
    }

    private MedlemskapOppgittLandOppholdEntitet byggUtlandsopphold(OppholdUtlandet utenlandsopphold, boolean tidligereOpphold) {
        return new MedlemskapOppgittLandOppholdEntitet.Builder()
            .medLand(finnLandkode(utenlandsopphold.getLand().getKode()))
            .medPeriode(
                utenlandsopphold.getPeriode().getFom(),
                utenlandsopphold.getPeriode().getTom()
            )
            .erTidligereOpphold(tidligereOpphold)
            .build();
    }

    private void settOppholdNorgePerioder(Medlemskap medlemskap, LocalDate mottattDato, MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder) {
        medlemskap.getOppholdNorge().forEach(opphNorge -> {
            boolean tidligereOpphold = opphNorge.getPeriode().getFom().isBefore(mottattDato);
            MedlemskapOppgittLandOppholdEntitet oppholdNorgeSistePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(tidligereOpphold)
                .medLand(Landkoder.NOR)
                .medPeriode(opphNorge.getPeriode().getFom(), opphNorge.getPeriode().getTom())
                .build();
            oppgittTilknytningBuilder.leggTilOpphold(oppholdNorgeSistePeriode);
        });
    }

    private Språkkode getSpraakValg(MottattDokumentWrapperSøknad skjema) {
        // default til bokmål om ikke satt
        if (skjema.getSpråkvalg() != null) {
            return Språkkode.fraKodeOptional(skjema.getSpråkvalg().getKode()).orElse(Språkkode.nb);
        }
        return Språkkode.nb;
    }

    private SøknadEntitet.Builder byggFelleselementerForSøknad(SøknadEntitet.Builder søknadBuilder, MottattDokumentWrapperSøknad skjemaWrapper, Boolean elektroniskSøknad, LocalDate forsendelseMottatt, Optional<LocalDate> gjelderFra) {
        søknadBuilder.medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(forsendelseMottatt)
            .medBegrunnelseForSenInnsending(skjemaWrapper.getBegrunnelseForSenSoeknad())
            .medTilleggsopplysninger(skjemaWrapper.getTilleggsopplysninger())
            .medSøknadsdato(gjelderFra.orElse(forsendelseMottatt))
            .medSpråkkode(getSpraakValg(skjemaWrapper));

        for (Vedlegg vedlegg : skjemaWrapper.getPåkrevdVedleggListe()) {
            byggSøknadVedlegg(søknadBuilder, vedlegg, true);
        }

        for (Vedlegg vedlegg : skjemaWrapper.getIkkePåkrevdVedleggListe()) {
            byggSøknadVedlegg(søknadBuilder, vedlegg, false);
        }

        return søknadBuilder;
    }

    private Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }

    private void byggSøknadVedlegg(SøknadEntitet.Builder søknadBuilder, Vedlegg vedlegg, boolean påkrevd) {
        SøknadVedleggEntitet.Builder vedleggBuilder = new SøknadVedleggEntitet.Builder()
            .medErPåkrevdISøknadsdialog(påkrevd)
            .medInnsendingsvalg(tolkInnsendingsvalg(vedlegg.getInnsendingstype()))
            .medSkjemanummer(vedlegg.getSkjemanummer())
            .medTilleggsinfo(vedlegg.getTilleggsinformasjon());
        søknadBuilder.leggTilVedlegg(vedleggBuilder.build());
    }

    private Innsendingsvalg tolkInnsendingsvalg(Innsendingstype innsendingstype) {
        // FIXME (MAUR) Slå opp mot kodeverk..
        switch (innsendingstype.getKode()) {
            case "IKKE_VALGT":
                return Innsendingsvalg.IKKE_VALGT;
            case "LASTET_OPP":
                return Innsendingsvalg.LASTET_OPP;
            case "SEND_SENERE":
                return Innsendingsvalg.SEND_SENERE;
            case "SENDES_IKKE":
                return Innsendingsvalg.SENDES_IKKE;
            case "VEDLEGG_ALLEREDE_SENDT":
                return Innsendingsvalg.VEDLEGG_ALLEREDE_SENDT;
            case "VEDLEGG_SENDES_AV_ANDRE":
                return Innsendingsvalg.VEDLEGG_SENDES_AV_ANDRE;
            default:
                return null;
        }
    }
}
