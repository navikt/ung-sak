package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode.ALDER_FOR_STRENGERE_PSB_VURDERING;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode.MAKSÅR;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.uttak.RettVedDødType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
public class HåndterHåndterePleietrengendeDødsfallTjenestePSB extends HåndterePleietrengendeDødsfallTjeneste {

    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;
    private boolean utvidVedDødsfall;

    HåndterHåndterePleietrengendeDødsfallTjenestePSB() {
        // CDI
    }

    @Inject
    public HåndterHåndterePleietrengendeDødsfallTjenestePSB(BehandlingRepository behandlingRepository,
                                                            VilkårResultatRepository vilkårResultatRepository,
                                                            @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                            @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) @BehandlingTypeRef VilkårUtleder vilkårUtleder,
                                                            PersonopplysningTjeneste personopplysningTjeneste,
                                                            RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository,
                                                            @KonfigVerdi(value = "PSB_PPN_UTVIDE_VED_DODSFALL", defaultVerdi = "true") boolean utvidVedDødsfall) {
        super(behandlingRepository, vilkårResultatRepository, vilkårUtleder, vilkårsPerioderTilVurderingTjeneste, personopplysningTjeneste);

        this.rettPleiepengerVedDødRepository = rettPleiepengerVedDødRepository;
        this.utvidVedDødsfall = utvidVedDødsfall;
    }


    public Optional<DatoIntervallEntitet> utledUtvidetPeriodeForDødsfall(BehandlingReferanse referanse) {
        if (!utvidVedDødsfall) {
            return Optional.empty();
        }

        var rettVedDødGrunnlagOpt = rettPleiepengerVedDødRepository.hentHvisEksisterer(referanse.getBehandlingId());
        if (rettVedDødGrunnlagOpt.isEmpty()) {
            return Optional.empty();
        }

        var rettVedDød = rettVedDødGrunnlagOpt.orElseThrow().getRettVedPleietrengendeDød().getRettVedDødType();

        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var pleietrengendePersonopplysninger = personopplysningerAggregat.getPersonopplysning(referanse.getPleietrengendeAktørId());

        var dødsdato = pleietrengendePersonopplysninger.getDødsdato();
        if (dødsdato == null) {
            return Optional.empty();
        }
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        if (harIkkeGodkjentSykdomPåDødsdatoen(dødsdato, vilkårene)) {
            return Optional.empty();
        }

        var utvidelseAvPeriode = utledUtvidelse(rettVedDød);
        var sisteDagPgaDødsfall = dødsdato.plus(utvidelseAvPeriode.getAntall(), utvidelseAvPeriode.getEnhet());

        return Optional.of(DatoIntervallEntitet.fraOgMedTilOgMed(dødsdato, sisteDagPgaDødsfall));
    }

    private UtvidelseAvPeriode utledUtvidelse(RettVedDødType rettVedDød) {
        return switch (rettVedDød) {
            case RETT_6_UKER -> new UtvidelseAvPeriode(6, ChronoUnit.WEEKS);
            case RETT_12_UKER -> new UtvidelseAvPeriode(3, ChronoUnit.MONTHS);
        };
    }

    protected void forlengMedisinskeVilkår(VilkårResultatBuilder resultatBuilder, Vilkårene vilkårene, DatoIntervallEntitet periode, LocalDate fødselsdato) {
        var set = new TreeSet<DatoIntervallEntitet>();
        set.add(periode);
        var perioderUnder18år = PleietrengendeAlderPeriode.utledPeriodeIHenhold(set, fødselsdato, -MAKSÅR, ALDER_FOR_STRENGERE_PSB_VURDERING);
        var perioderOver18år = PleietrengendeAlderPeriode.utledPeriodeIHenhold(set, fødselsdato, ALDER_FOR_STRENGERE_PSB_VURDERING, MAKSÅR);

        var eksisterendeResultat = finnSykdomVurderingPåDødsdato(periode.getFomDato(), perioderUnder18år, perioderOver18år, vilkårene);

        if (!perioderUnder18år.isEmpty()) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
            leggTilPerioder(resultatBuilder, perioderUnder18år, eksisterendeResultat, vilkårBuilder);
        }
        if (!perioderOver18år.isEmpty()) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
            leggTilPerioder(resultatBuilder, perioderOver18år, eksisterendeResultat, vilkårBuilder);
        }
    }

    private void leggTilPerioder(VilkårResultatBuilder resultatBuilder, NavigableSet<DatoIntervallEntitet> perioder, VilkårPeriode eksisterendeResultat, VilkårBuilder vilkårBuilder) {
        for (DatoIntervallEntitet intervall : perioder) {
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(intervall).forlengelseAv(eksisterendeResultat));
        }

        resultatBuilder.leggTil(vilkårBuilder);
    }

    private VilkårPeriode finnSykdomVurderingPåDødsdato(LocalDate dødsdato, NavigableSet<DatoIntervallEntitet> perioderUnder18år, NavigableSet<DatoIntervallEntitet> perioderOver18år, Vilkårene vilkårene) {
        if (perioderUnder18år.stream().anyMatch(it -> it.inkluderer(dødsdato))) {
            return vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR).orElseThrow().finnPeriodeSomInneholderDato(dødsdato).orElseThrow();
        } else if (perioderOver18år.stream().anyMatch(it -> it.inkluderer(dødsdato))) {
            return vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR).orElseThrow().finnPeriodeSomInneholderDato(dødsdato).orElseThrow();
        }
        throw new IllegalStateException("Fant ikke overlapp verken i over eller under 18");
    }

}
