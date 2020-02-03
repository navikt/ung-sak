package no.nav.foreldrepenger.domene.registerinnhenting;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;

@Dependent
public class EndringsresultatSjekker {

    private PersonopplysningTjeneste personopplysningTjeneste;
    private MedlemTjeneste medlemTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    private OpptjeningRepository opptjeningRepository;
    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    EndringsresultatSjekker() {
        // For CDI
    }

    @Inject
    public EndringsresultatSjekker(PersonopplysningTjeneste personopplysningTjeneste,
                                   MedlemTjeneste medlemTjeneste,
                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                   BehandlingRepositoryProvider provider) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.medlemTjeneste = medlemTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.opptjeningRepository = provider.getOpptjeningRepository();
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.vilkårResultatRepository = provider.getVilkårResultatRepository();
    }

    public EndringsresultatSnapshot opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(Long behandlingId) {
        EndringsresultatSnapshot snapshot = EndringsresultatSnapshot.opprett();
        snapshot.leggTil(personopplysningTjeneste.finnAktivGrunnlagId(behandlingId));
        snapshot.leggTil(medlemTjeneste.finnAktivGrunnlagId(behandlingId));

        EndringsresultatSnapshot iaySnapshot =  inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId)
                .map(iayg -> EndringsresultatSnapshot.medSnapshot(InntektArbeidYtelseGrunnlag.class, iayg.getEksternReferanse()))
                .orElse(EndringsresultatSnapshot.utenSnapshot(InntektArbeidYtelseGrunnlag.class));

        snapshot.leggTil(iaySnapshot);

        return snapshot;
    }

    public EndringsresultatDiff finnSporedeEndringerPåBehandlingsgrunnlag(Long behandlingId, EndringsresultatSnapshot idSnapshotFør) {
        final boolean kunSporedeEndringer = true;
        // Del 1: Finn diff mellom grunnlagets id før og etter oppdatering
        EndringsresultatSnapshot idSnapshotNå = opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandlingId);
        EndringsresultatDiff idDiff = idSnapshotNå.minus(idSnapshotFør);

        // Del 2: Transformer diff på grunnlagets id til diff på grunnlagets sporede endringer (@ChangeTracked)
        EndringsresultatDiff sporedeEndringerDiff = EndringsresultatDiff.opprettForSporingsendringer();
        idDiff.hentDelresultat(PersonInformasjonEntitet.class).ifPresent(idEndring ->
            sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> personopplysningTjeneste.diffResultat(idEndring, kunSporedeEndringer)));
        idDiff.hentDelresultat(MedlemskapAggregat.class).ifPresent(idEndring ->
            sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> medlemTjeneste.diffResultat(idEndring, kunSporedeEndringer)));
        idDiff.hentDelresultat(InntektArbeidYtelseGrunnlag.class).ifPresent(idEndring ->
            sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> {
                InntektArbeidYtelseGrunnlag grunnlag1 = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, (UUID)idEndring.getGrunnlagId1());
                InntektArbeidYtelseGrunnlag grunnlag2 = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, (UUID)idEndring.getGrunnlagId2());
                return new IAYGrunnlagDiff(grunnlag1, grunnlag2).diffResultat(kunSporedeEndringer);
            }));
        return sporedeEndringerDiff;
    }

    public EndringsresultatSnapshot opprettEndringsresultatIdPåBehandlingSnapshot(Behandling behandling) {
        Long behandlingId = behandling.getId();
        EndringsresultatSnapshot snapshot = opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandlingId);

        snapshot.leggTil(opptjeningRepository.finnAktivGrunnlagId(behandling));
        snapshot.leggTil(finnAktivBeregningsgrunnlagGrunnlagAggregatId(behandlingId));

        // Resultatstrukturene nedenfor støtter ikke paradigme med "aktivt" grunnlag som kan identifisere med id
        // Aksepterer her at endringssjekk heller utledes av deres tidsstempel forutsatt at metoden ikke brukes i
        // kritiske endringssjekker. Håp om at de i fremtiden vil støtte paradigme.
        snapshot.leggTil(lagVilkårResultatIdSnapshotAvTidsstempel(behandling));

        return snapshot;
    }

    public EndringsresultatDiff finnIdEndringerPåBehandling(Behandling behandling, EndringsresultatSnapshot idSnapshotFør) {
        EndringsresultatSnapshot idSnapshotNå = opprettEndringsresultatIdPåBehandlingSnapshot(behandling);
        return idSnapshotNå.minus(idSnapshotFør);
    }

    private EndringsresultatSnapshot lagVilkårResultatIdSnapshotAvTidsstempel(Behandling behandling) {
       return vilkårResultatRepository.hentHvisEksisterer(behandling.getId())
                 .map(vilkårResultat ->
                     EndringsresultatSnapshot.medSnapshot(Vilkårene.class, hentLongVerdiAvEndretTid(vilkårResultat)))
                 .orElse(EndringsresultatSnapshot.utenSnapshot(Vilkårene.class));
    }

    //Denne metoden bør legges i Tjeneste
    public EndringsresultatSnapshot finnAktivBeregningsgrunnlagGrunnlagAggregatId(Long behandlingId) {
        Optional<Long> aktivBeregningsgrunnlagGrunnlagId = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId).map(BeregningsgrunnlagGrunnlagEntitet::getId);
        return aktivBeregningsgrunnlagGrunnlagId
            .map(id -> EndringsresultatSnapshot.medSnapshot(BeregningsgrunnlagEntitet.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(BeregningsgrunnlagEntitet.class));
    }


    private Long hentLongVerdiAvEndretTid(BaseEntitet entitet) {
       LocalDateTime endretTidspunkt = entitet.getOpprettetTidspunkt();
       if(entitet.getEndretTidspunkt()!=null){
           endretTidspunkt = entitet.getEndretTidspunkt();
       }
       return mapFraLocalDateTimeTilLong(endretTidspunkt);
    }

    static Long mapFraLocalDateTimeTilLong(LocalDateTime ldt){
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("Europe/Paris"));
        return zdt.toInstant().toEpochMilli();
    }
}
