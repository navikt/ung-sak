package no.nav.ung.sak.web.app.tjenester.behandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandling.revurdering.RevurderingFeil;
import no.nav.ung.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.klage.domenetjenester.KlageVurderingTjeneste;
import no.nav.ung.sak.kontrakt.behandling.ÅrsakOgPerioderDto;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.typer.Saksnummer;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static no.nav.k9.felles.feil.LogLevel.INFO;

@ApplicationScoped
public class BehandlingsoppretterTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private Instance<GyldigePerioderForRevurderingPrÅrsakUtleder> gyldigePerioderForRevurderingUtledere;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;


    BehandlingsoppretterTjeneste() {
        // CDI
    }

    @Inject
    public BehandlingsoppretterTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                        BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                        @Any Instance<GyldigePerioderForRevurderingPrÅrsakUtleder> gyldigePerioderForRevurderingUtledere,
                                        BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                        KlageVurderingTjeneste klageVurderingTjeneste,
                                        PersonopplysningRepository personopplysningRepository,
                                        HistorikkinnslagRepository historikkinnslagRepository) {
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.gyldigePerioderForRevurderingUtledere = gyldigePerioderForRevurderingUtledere;
        Objects.requireNonNull(behandlingRepositoryProvider, "behandlingRepositoryProvider");
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.personopplysningRepository = personopplysningRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<DatoIntervallEntitet> periode) {
        return opprettRevurdering(fagsak, behandlingÅrsakType, periode, true);
    }

    public Behandling opprettAutomatiskRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        return opprettRevurdering(fagsak, behandlingÅrsakType, Optional.empty(), false);
    }

    private Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<DatoIntervallEntitet> periode, boolean manuell) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        var kanRevurderingOpprettes = kanOppretteRevurdering(fagsak.getId());
        if (!kanRevurderingOpprettes) {
            throw BehandlingsoppretterTjeneste.BehandlingsoppretterTjenesteFeil.FACTORY.kanIkkeOppretteRevurdering(fagsak.getSaksnummer()).toException();
        }
        if (!periodeKanRevurderesForÅrsak(fagsak, behandlingÅrsakType, periode)) {
            throw new IllegalArgumentException("Perioden er ikke tidligere kontrollert");
        }

        var origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(fagsak.getId())
            .orElseThrow(() -> RevurderingFeil.FACTORY.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()).toException());

        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        return manuell
            ? revurderingTjeneste.opprettManuellRevurdering(origBehandling, behandlingÅrsakType, enhet, periode)
            : revurderingTjeneste.opprettAutomatiskRevurdering(origBehandling, behandlingÅrsakType, enhet);
    }


    public Behandling opprettKlageBehandling(Fagsak fagsak) {
        var forrigeBehandling = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId()).orElseThrow(
            () -> new IllegalStateException("Kan ikke opprette klagebehandling uten tidligere behandling")
        );

        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        var nyBehandling = opprettBehandling(fagsak, BehandlingType.KLAGE, enhet, BehandlingÅrsakType.UDEFINERT);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(forrigeBehandling.getId(), nyBehandling.getId());
        klageVurderingTjeneste.opprettKlageUtredning(nyBehandling, enhet);

        opprettHistorikkinnslag(nyBehandling, true);
        return nyBehandling;
    }

    private Behandling opprettBehandling(Fagsak fagsak, BehandlingType behandlingType, OrganisasjonsEnhet enhet, BehandlingÅrsakType årsak) {
        return behandlingskontrollTjeneste.opprettNyBehandling(fagsak, behandlingType,
            beh -> {
                if (!BehandlingÅrsakType.UDEFINERT.equals(årsak)) {
                    BehandlingÅrsak.builder(årsak).buildFor(beh);
                }
                beh.setBehandlingstidFrist(LocalDate.now().plusWeeks(behandlingType.getBehandlingstidFristUker()));
                beh.setBehandlendeEnhet(enhet);
            });
    }

    public boolean kanOppretteNyBehandlingAvType(Long fagsakId, BehandlingType type) {
        boolean finnesÅpneBehandlingerAvType = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsakId, type).size() > 0;
        if (finnesÅpneBehandlingerAvType) {
            return false;
        }
        switch (type) {
            case FØRSTEGANGSSØKNAD:
                return kanOppretteFørstegangsbehandling(fagsakId);
            case REVURDERING:
                return kanOppretteRevurdering(fagsakId);
            default:
                return false;
        }
    }

    public List<ÅrsakOgPerioderDto> finnGyldigeVurderingsperioderPrÅrsak(Long fagsakId) {
        return gyldigePerioderForRevurderingUtledere.stream().map(utleder -> utleder.utledPerioder(fagsakId))
            .toList();
    }

    private boolean kanOppretteFørstegangsbehandling(Long fagsakId) {
        return false; // Ikke støttet i k9
    }

    private boolean kanOppretteRevurdering(Long fagsakId) {
        var åpneBehandlinger = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsakId, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingType.REVURDERING);
        //Strengere versjon var behandling = behandlingRepository.finnSisteInnvilgetBehandling(fagsakId).orElse(null);
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElse(null);
        if (åpneBehandlinger.size() > 0 || behandling == null) {
            return false;
        }
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsakYtelseType()).orElseThrow();
        return revurderingTjeneste.kanRevurderingOpprettes(behandling.getFagsak());
    }

    interface BehandlingsoppretterTjenesteFeil extends DeklarerteFeil {
        BehandlingsoppretterTjenesteFeil FACTORY = FeilFactory.create(BehandlingsoppretterTjenesteFeil.class); // NOSONAR

        @FunksjonellFeil(feilkode = "FP-663487", feilmelding = "Fagsak med saksnummer %s oppfyller ikke kravene for revurdering", løsningsforslag = "", logLevel = INFO)
        Feil kanIkkeOppretteRevurdering(Saksnummer saksnummer);

    }

    public void opprettHistorikkinnslag(Behandling behandling, boolean manueltOpprettet) {
        HistorikkAktør historikkAktør = manueltOpprettet ? HistorikkAktør.SAKSBEHANDLER : HistorikkAktør.VEDTAKSLØSNINGEN;

        var historikkBuilder = new Historikkinnslag.Builder();
        historikkBuilder.medTittel("Klagebehandling opprettet")
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medAktør(historikkAktør);

        historikkinnslagRepository.lagre(historikkBuilder.build());
    }

    private boolean periodeKanRevurderesForÅrsak(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<DatoIntervallEntitet> periode) {
        var gyldigePerioderForRevurderingPrÅrsak = finnGyldigeVurderingsperioderPrÅrsak(fagsak.getId());
        boolean skalSjekkeGyldighetAvPeriode = gyldigePerioderForRevurderingPrÅrsak.stream().anyMatch(dto -> dto.årsak() == behandlingÅrsakType);
        //Dersom det ikke er utledet gyldige perioder for årsak så aksepteres alle perioder, også ingen periode.
        if (!skalSjekkeGyldighetAvPeriode) {
            return true;
        }
        if (periode.isEmpty()) {
            return false;
        }
        return gyldigePerioderForRevurderingPrÅrsak.stream().filter(
                dto -> dto.årsak() == behandlingÅrsakType).flatMap(dto -> dto.perioder().stream())
            .map(DatoIntervallEntitet::fra)
            .anyMatch(periode.get()::equals);
    }

}
