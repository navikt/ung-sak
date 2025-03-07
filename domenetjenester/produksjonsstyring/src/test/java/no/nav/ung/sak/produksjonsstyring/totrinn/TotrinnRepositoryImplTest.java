package no.nav.ung.sak.produksjonsstyring.totrinn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.AktørId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class TotrinnRepositoryImplTest {

    private static final AksjonspunktDefinisjon AKSDEF_01 = AksjonspunktDefinisjon.values()[0];
    private static final AksjonspunktDefinisjon AKSDEF_02 = AksjonspunktDefinisjon.values()[1];
    private static final AksjonspunktDefinisjon AKSDEF_03 = AksjonspunktDefinisjon.values()[2];


    @Inject
    private EntityManager entityManager;

    private TotrinnRepository totrinnRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void oppsett() {
        totrinnRepository = new TotrinnRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    public void skal_finne_ett_inaktivt_totrinnsgrunnlag_og_ett_aktivt_totrinnsgrunnlag() {

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        Totrinnresultatgrunnlag gammeltTotrinnresultatgrunnlag = new Totrinnresultatgrunnlag(behandling,
            null);

        Totrinnresultatgrunnlag nyttTotrinnresultatgrunnlag = new Totrinnresultatgrunnlag(behandling,
            null);

        totrinnRepository.lagreOgFlush(behandling, gammeltTotrinnresultatgrunnlag);
        totrinnRepository.lagreOgFlush(behandling, nyttTotrinnresultatgrunnlag);

        // Hent ut aktiv totrinnsgrunnlag
        Optional<Totrinnresultatgrunnlag> optionalNyttTotrinnresultatgrunnlag = totrinnRepository.hentTotrinngrunnlag(behandling);

        // Hent ut inaktive totrinnsgrunnlag
        TypedQuery<Totrinnresultatgrunnlag> query = entityManager.createQuery(
            "SELECT trg FROM Totrinnresultatgrunnlag trg WHERE trg.behandling.id = :behandling_id AND trg.aktiv = false", //$NON-NLS-1$
            Totrinnresultatgrunnlag.class);
        query.setParameter("behandling_id", behandling.getId()); //$NON-NLS-1$
        List<Totrinnresultatgrunnlag> inaktive = query.getResultList();

        assertThat(inaktive).hasSize(1);
        assertThat(inaktive.get(0)).isEqualToComparingFieldByField(gammeltTotrinnresultatgrunnlag);
        assertThat(optionalNyttTotrinnresultatgrunnlag).isPresent();
        assertThat(optionalNyttTotrinnresultatgrunnlag.get().getId()).isNotEqualTo(gammeltTotrinnresultatgrunnlag.getId());
        assertThat(optionalNyttTotrinnresultatgrunnlag.get().isAktiv()).isTrue();

    }

    @Test
    public void skal_finne_flere_inaktive_totrinnsvurderinger_og_flere_aktive_totrinnsvurdering() {

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        // Opprett vurderinger som skal være inaktive
        Totrinnsvurdering inaktivTotrinnsvurdering1 = lagTotrinnsvurdering(behandling,
            AKSDEF_01, true, "", VurderÅrsak.FEIL_FAKTA);
        Totrinnsvurdering inaktivTotrinnsvurdering2 = lagTotrinnsvurdering(behandling,
            AKSDEF_02, true, "", VurderÅrsak.FEIL_FAKTA);
        Totrinnsvurdering inaktivTotrinnsvurdering3 = lagTotrinnsvurdering(behandling,
            AKSDEF_03, true, "", VurderÅrsak.FEIL_FAKTA);

        List<Totrinnsvurdering> inaktivTotrinnsvurderingList = new ArrayList<>();
        inaktivTotrinnsvurderingList.add(inaktivTotrinnsvurdering1);
        inaktivTotrinnsvurderingList.add(inaktivTotrinnsvurdering2);
        inaktivTotrinnsvurderingList.add(inaktivTotrinnsvurdering3);
        totrinnRepository.lagreOgFlush(behandling, inaktivTotrinnsvurderingList);

        // Opprett vurderinger som skal være aktive
        Totrinnsvurdering aktivTotrinnsvurdering1 = lagTotrinnsvurdering(behandling,
            AKSDEF_01, false, "", VurderÅrsak.FEIL_FAKTA);
        Totrinnsvurdering aktivTotrinnsvurdering2 = lagTotrinnsvurdering(behandling,
            AKSDEF_02, false, "", VurderÅrsak.FEIL_FAKTA);
        Totrinnsvurdering aktivTotrinnsvurdering3 = lagTotrinnsvurdering(behandling,
            AKSDEF_03, false, "", VurderÅrsak.FEIL_FAKTA);

        List<Totrinnsvurdering> aktivTotrinnsvurderingList = new ArrayList<>();
        aktivTotrinnsvurderingList.add(aktivTotrinnsvurdering1);
        aktivTotrinnsvurderingList.add(aktivTotrinnsvurdering2);
        aktivTotrinnsvurderingList.add(aktivTotrinnsvurdering3);
        totrinnRepository.lagreOgFlush(behandling, aktivTotrinnsvurderingList);

        // Hent aktive vurderinger etter flush
        Collection<Totrinnsvurdering> repoAktiveTotrinnsvurderinger = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling);

        // Hent inaktive vurderinger etter flush
        TypedQuery<Totrinnsvurdering> query = entityManager.createQuery(
            "SELECT tav FROM Totrinnsvurdering tav WHERE tav.behandling.id = :behandling_id AND tav.aktiv = false", //$NON-NLS-1$
            Totrinnsvurdering.class);
        query.setParameter("behandling_id", behandling.getId()); //$NON-NLS-1$
        List<Totrinnsvurdering> repoInaktiveTotrinnsvurderinger = query.getResultList();

        // Sjekk lagrede aktive vurderinger
        assertThat(repoAktiveTotrinnsvurderinger).hasSize(3);
        repoAktiveTotrinnsvurderinger.forEach(totrinnsvurdering -> assertThat(totrinnsvurdering.isAktiv()).isTrue());

        // Sjekk lagrede inaktive vurderinger
        assertThat(repoInaktiveTotrinnsvurderinger).hasSize(3);
        repoInaktiveTotrinnsvurderinger.forEach(totrinnsvurdering -> assertThat(totrinnsvurdering.isAktiv()).isFalse());

    }

    private Totrinnsvurdering lagTotrinnsvurdering(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                                   boolean godkjent, String begrunnelse, VurderÅrsak vurderÅrsak) {
        return new Totrinnsvurdering.Builder(behandling, aksjonspunktDefinisjon)
            .medGodkjent(godkjent)
            .medBegrunnelse(begrunnelse)
            .medVurderÅrsak(vurderÅrsak)
            .build();
    }
}
