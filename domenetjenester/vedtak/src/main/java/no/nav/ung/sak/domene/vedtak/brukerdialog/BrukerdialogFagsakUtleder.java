package no.nav.ung.sak.domene.vedtak.brukerdialog;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.brukerdialog.kontrakt.vedtak.FagSakRequest;
import no.nav.ung.brukerdialog.kontrakt.vedtak.MottattSøknadDto;
import no.nav.ung.brukerdialog.kontrakt.vedtak.VedtakPeriodeDto;
import no.nav.ung.brukerdialog.kontrakt.vedtak.VedtakResultatType;
import no.nav.ung.brukerdialog.typer.AktørId;
import no.nav.ung.brukerdialog.typer.Periode;
import no.nav.ung.brukerdialog.typer.Saksnummer;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Dependent
public class BrukerdialogFagsakUtleder {

    private static final Logger log = LoggerFactory.getLogger(BrukerdialogFagsakUtleder.class);

    private final BehandlingRepository behandlingRepository;
    private final SøknadRepository søknadRepository;
    private final VilkårTjeneste vilkårTjeneste;

    @Inject
    public BrukerdialogFagsakUtleder(BehandlingRepository behandlingRepository,
                                     SøknadRepository søknadRepository,
                                     VilkårTjeneste vilkårTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.søknadRepository = søknadRepository;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    public FagSakRequest utled(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();

        return new FagSakRequest(
            new AktørId(fagsak.getAktørId().getId()),
            new Saksnummer(fagsak.getSaksnummer().getVerdi()),
            vedtaksperioder(behandling),
            mottatteSøknader(fagsak.getId()));
    }

    private List<VedtakPeriodeDto> vedtaksperioder(Behandling behandling) {
        LocalDateTimeline<VilkårUtfallSamlet> tidslinje = vilkårTjeneste.samletVilkårsresultat(behandling.getId());
        return tidslinje
            .mapValue(VilkårUtfallSamlet::getSamletUtfall)
            .filterValue(it -> it == Utfall.OPPFYLT || it == Utfall.IKKE_OPPFYLT)
            .compress()
            .stream()
            .map(segment ->
                new VedtakPeriodeDto(new Periode(segment.getFom(), segment.getTom()), mapUtfall(segment.getValue())))
            .toList();
    }

    private static VedtakResultatType mapUtfall(Utfall samletUtfall) {
        return switch (samletUtfall) {
            case OPPFYLT -> VedtakResultatType.INNVILGET;
            case IKKE_OPPFYLT -> VedtakResultatType.AVSLÅTT;
            default-> throw new IllegalStateException("Kan ikke mappe vilkårsutfall " + samletUtfall + " til vedtaksresultat");
        };
    }

    private List<MottattSøknadDto> mottatteSøknader(Long fagsakId) {
        return søknadRepository.hentSøknaderForFagsak(fagsakId).stream()
            .map(this::tilMottattSøknadDto)
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<MottattSøknadDto> tilMottattSøknadDto(SøknadEntitet søknad) {
        return parseSøknadId(søknad.getSøknadId())
            .map(søknadId -> new MottattSøknadDto(
                søknadId,
                søknad.getMottattDato()));
    }

    private Optional<UUID> parseSøknadId(String søknadId) {
        if (søknadId == null || søknadId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(søknadId));
        } catch (IllegalArgumentException e) {
            log.info("SøknadId {} er ikke en UUID og kan ikke kobles mot en søknadshendelse.", søknadId);
            return Optional.empty();
        }
    }
}
