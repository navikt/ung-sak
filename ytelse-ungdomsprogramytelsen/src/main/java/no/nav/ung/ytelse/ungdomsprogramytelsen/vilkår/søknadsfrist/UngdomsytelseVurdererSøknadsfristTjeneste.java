package no.nav.ung.ytelse.ungdomsprogramytelsen.vilkår.søknadsfrist;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.VurdertSøktPeriode;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.søknadsfrist.KravDokument;
import no.nav.ung.sak.søknadsfrist.KravDokumentType;
import no.nav.ung.sak.søknadsfrist.SøktPeriode;
import no.nav.ung.sak.søknadsfrist.VurderSøknadsfristTjeneste;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class UngdomsytelseVurdererSøknadsfristTjeneste implements VurderSøknadsfristTjeneste<SøktStartdato> {

    // TODO: Denne må implementeres

    private MottatteDokumentRepository mottatteDokumentRepository;
    private StartdatoRepository startdatoRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    UngdomsytelseVurdererSøknadsfristTjeneste() {
        // CDI
    }

    @Inject
    public UngdomsytelseVurdererSøknadsfristTjeneste(MottatteDokumentRepository mottatteDokumentRepository, StartdatoRepository startdatoRepository, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.startdatoRepository = startdatoRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }


    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<SøktStartdato>>> vurderSøknadsfrist(BehandlingReferanse referanse) {
        var søktePerioder = hentPerioderTilVurdering(referanse);

        return vurderSøknadsfrist(referanse.getBehandlingId(), søktePerioder);
    }

    @Override
    public Map<KravDokument, List<SøktPeriode<SøktStartdato>>> hentPerioderTilVurdering(BehandlingReferanse referanse) {
        var result = new HashMap<KravDokument, List<SøktPeriode<SøktStartdato>>>();

        var mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(referanse.getFagsakId())
            .stream()
            .filter(it -> Brevkode.UNGDOMSYTELSE_SOKNAD.equals(it.getType()))
            .collect(Collectors.toSet());

        if (mottatteDokumenter.isEmpty()) {
            return result;
        }

        var ungdomsprogramperioder = ungdomsprogramPeriodeRepository.hentGrunnlag(referanse.getBehandlingId())
            .stream()
            .map(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramPerioder)
            .map(UngdomsprogramPerioder::getPerioder)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        startdatoRepository.hentGrunnlag(referanse.getBehandlingId())
            .stream()
            .map(StartdatoGrunnlag::getOppgitteStartdatoer)
            .map(Startdatoer::getStartdatoer)
            .flatMap(Collection::stream)
            .filter(it -> mottatteDokumenter.stream().anyMatch(at -> at.getJournalpostId().equals(it.getJournalpostId())))
            .forEach(dokument -> mapTilKravDokumentOgPeriode(result, mottatteDokumenter, ungdomsprogramperioder, dokument));

        return result;
    }

    private void mapTilKravDokumentOgPeriode(HashMap<KravDokument, List<SøktPeriode<SøktStartdato>>> result, Set<MottattDokument> mottatteDokumenter, Set<UngdomsprogramPeriode> ungdomsprogramperioder, SøktStartdato dokument) {
        var mottattDokument = mottatteDokumenter.stream()
            .filter(it -> it.getJournalpostId().equals(dokument.getJournalpostId()))
            .findFirst().orElseThrow();
        var ungdomsprogramPeriode = ungdomsprogramperioder.stream().filter(it -> it.getPeriode().getFomDato().equals(dokument.getStartdato())).findFirst();
        var kravDokument = new KravDokument(dokument.getJournalpostId(), mottattDokument.getInnsendingstidspunkt(), KravDokumentType.SØKNAD, mottattDokument.getKildesystem());
        ungdomsprogramPeriode.ifPresentOrElse((p) -> result.put(kravDokument, List.of(new SøktPeriode<>(p.getPeriode(), dokument))), () -> result.put(kravDokument, List.of()));
    }


    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<SøktStartdato>>> vurderSøknadsfrist(Long behandlingId, Map<KravDokument, List<SøktPeriode<SøktStartdato>>> søknaderMedPerioder) {
        var result = new HashMap<KravDokument, List<VurdertSøktPeriode<SøktStartdato>>>();
        return result;
    }

    @Override
    public Set<KravDokument> relevanteKravdokumentForBehandling(BehandlingReferanse referanse, boolean taHensynTilManuellRevurdering) {
        return mottatteDokumentRepository.hentMottatteDokumentForBehandling(referanse.getFagsakId(), referanse.getBehandlingId(), List.of(Brevkode.UNGDOMSYTELSE_SOKNAD), false, DokumentStatus.GYLDIG)
            .stream()
            .map(it -> new KravDokument(it.getJournalpostId(), it.getInnsendingstidspunkt(), KravDokumentType.SØKNAD, it.getKildesystem()))
            .collect(Collectors.toSet());
    }

}
