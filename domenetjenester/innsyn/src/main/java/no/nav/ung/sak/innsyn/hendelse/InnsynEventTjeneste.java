package no.nav.ung.sak.innsyn.hendelse;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.innsyn.InnsynHendelse;
import no.nav.k9.innsyn.sak.Aksjonspunkt;
import no.nav.k9.innsyn.sak.AktørId;
import no.nav.k9.innsyn.sak.BehandlingResultat;
import no.nav.k9.innsyn.sak.InnsendingInfo;
import no.nav.k9.innsyn.sak.InnsendingStatus;
import no.nav.k9.innsyn.sak.InnsendingType;
import no.nav.k9.innsyn.sak.Saksnummer;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.innsyn.BrukerdialoginnsynMeldingProducer;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.felles.Kildesystem;

@Dependent
public class InnsynEventTjeneste {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final MottatteDokumentRepository mottatteDokumentRepository;
    private final BrukerdialoginnsynMeldingProducer producer;

    @Inject
    public InnsynEventTjeneste(
            MottatteDokumentRepository mottatteDokumentRepository,
            BrukerdialoginnsynMeldingProducer producer
    ) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.producer = producer;
    }


    public void publiserBehandling(Behandling behandling) {
        Fagsak fagsak = behandling.getFagsak();
        if (fagsak.getYtelseType() != FagsakYtelseType.PSB) {
            return;
        }

        Set<Aksjonspunkt> aksjonspunkter = mapAksjonspunkter(behandling);
        var behandlingInnsyn = new no.nav.k9.innsyn.sak.Behandling(
            behandling.getUuid(),
            behandling.getOpprettetDato().atZone(ZoneId.systemDefault()),
            Optional.ofNullable(behandling.getAvsluttetDato()).map(it -> it.atZone(ZoneId.systemDefault())).orElse(null),
            mapBehandlingResultat(behandling.getBehandlingResultatType()),
            mapBehandingStatus(behandling, aksjonspunkter),
            mapSøknaderOgEttersendelser(behandling),
            aksjonspunkter,
            false,
            mapFagsak(fagsak)
        );

        String json = JsonUtils.toString(new InnsynHendelse<>(ZonedDateTime.now(), behandlingInnsyn));

        producer.send(fagsak.getSaksnummer().getVerdi(), json);
        log.info("Publisert behandling til brukerdialog");
    }

    private Set<Aksjonspunkt> mapAksjonspunkter(Behandling b) {
        return b.getÅpneAksjonspunkter().stream()
            .map(this::mapRelevanteAksjonspunkterPåVent)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    }


    private Optional<Aksjonspunkt> mapRelevanteAksjonspunkterPåVent(no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt it) {
        var venteårsak = it.getVenteårsak();
        if (venteårsak == null) {
            return Optional.empty();
        }

        var frist = it.getFristTid() != null ? it.getFristTid().atZone(ZoneId.systemDefault()) : null;

        return switch (venteårsak) {
            case FOR_TIDLIG_SOKNAD -> lagAksjonspunktForInnsyn(frist, Aksjonspunkt.Venteårsak.FOR_TIDLIG_SOKNAD);
            default -> Optional.empty();
        };
    }

    private static Optional<Aksjonspunkt> lagAksjonspunktForInnsyn(ZonedDateTime frist, Aksjonspunkt.Venteårsak venteårsak) {
        return Optional.of(new Aksjonspunkt(venteårsak, frist));
    }


    private no.nav.k9.innsyn.sak.Fagsak mapFagsak(Fagsak fagsak) {
        return new no.nav.k9.innsyn.sak.Fagsak(new Saksnummer(fagsak.getSaksnummer().getVerdi()), new AktørId(fagsak.getAktørId().getId()), null, no.nav.k9.innsyn.sak.FagsakYtelseType.fraKode(fagsak.getYtelseType().getKode()));
    }

    private Set<InnsendingInfo> mapSøknaderOgEttersendelser(Behandling b) {
        List<MottattDokument> mottattDokuments = mottatteDokumentRepository.hentMottatteDokumentForBehandling(b.getFagsakId(), b.getId(),
            List.of(
                Brevkode.PLEIEPENGER_BARN_SOKNAD,
                Brevkode.ETTERSENDELSE_PLEIEPENGER_SYKT_BARN
            ),
            false, DokumentStatus.BEHANDLER, DokumentStatus.GYLDIG, DokumentStatus.MOTTATT);
        return mottattDokuments.stream()
            .map(it -> new InnsendingInfo(
                InnsendingStatus.MOTTATT,
                it.getJournalpostId().getVerdi(),
                it.getMottattTidspunkt().atZone(ZoneId.systemDefault()),
                Optional.ofNullable(it.getKildesystem()).map(Kildesystem::of).orElse(null),
                mapInnsendingType(it.getType())
            ))
            .collect(Collectors.toSet());
    }

    private static InnsendingType mapInnsendingType(Brevkode type) {
        if (type.equals(Brevkode.PLEIEPENGER_BARN_SOKNAD)) {
            return InnsendingType.SØKNAD;
        }
        if (type.equals(Brevkode.ETTERSENDELSE_PLEIEPENGER_SYKT_BARN)) {
            return InnsendingType.ETTERSENDELSE;
        }
        //Bør ikke skje da dokumentene hentes i tidligere steg.
        throw new IllegalStateException("Støtter ikke brevkode %s".formatted(type.getKode()));
    }

    private no.nav.k9.innsyn.sak.BehandlingStatus mapBehandingStatus(Behandling behandling, Set<Aksjonspunkt> aksjonspunkter) {
        if (!aksjonspunkter.isEmpty()) {
            return no.nav.k9.innsyn.sak.BehandlingStatus.PÅ_VENT;
        }
        if (behandling.getStatus() == BehandlingStatus.AVSLUTTET) {
            return no.nav.k9.innsyn.sak.BehandlingStatus.AVSLUTTET;
        }
        return no.nav.k9.innsyn.sak.BehandlingStatus.UNDER_BEHANDLING;
    }
    private BehandlingResultat mapBehandlingResultat(BehandlingResultatType behandlingResultatType) {
        if (behandlingResultatType.erHenleggelse()) {
            return BehandlingResultat.HENLAGT;
        }

        if (behandlingResultatType == BehandlingResultatType.AVSLÅTT) {
            return BehandlingResultat.AVSLÅTT;
        }

        if (behandlingResultatType == BehandlingResultatType.INNVILGET) {
            return BehandlingResultat.INNVILGET;
        }


        if (behandlingResultatType == BehandlingResultatType.DELVIS_INNVILGET) {
            return BehandlingResultat.DELVIS_INNVILGET;
        }

        return null;
    }
}
