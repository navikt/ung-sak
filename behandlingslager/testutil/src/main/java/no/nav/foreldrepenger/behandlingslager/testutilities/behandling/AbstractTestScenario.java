package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import no.nav.foreldrepenger.behandlingslager.aktør.BrukerTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapBehandlingsgrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRegistrertEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårsResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personstatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.Whitebox;

/**
 * Default test scenario builder for å definere opp testdata med enkle defaults.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne
 * klassen.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractTestScenario<S extends AbstractTestScenario<S>> {

    private static final AtomicLong FAKE_ID = new AtomicLong(100999L);
    private final FagsakBuilder fagsakBuilder;
    private final Map<Long, PersonopplysningGrunnlagEntitet> personopplysningMap = new IdentityHashMap<>();
    private final Map<Long, MedlemskapBehandlingsgrunnlagEntitet> medlemskapgrunnlag = new HashMap<>();
    private final Map<Long, Behandling> behandlingMap = new HashMap<>();
    private ArgumentCaptor<Behandling> behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);
    private ArgumentCaptor<Fagsak> fagsakCaptor = ArgumentCaptor.forClass(Fagsak.class);
    private Behandling behandling;

    private Behandlingsresultat.Builder behandlingresultatBuilder;

    private Fagsak fagsak;
    private SøknadEntitet.Builder søknadBuilder;

    private BehandlingVedtak.Builder behandlingVedtakBuilder;
    private MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder;
    private BehandlingStegType startSteg;

    private Map<AksjonspunktDefinisjon, BehandlingStegType> aksjonspunktDefinisjoner = new HashMap<>();
    private Map<VilkårType, Utfall> vilkårTyper = new HashMap<>();
    private List<MedlemskapPerioderEntitet> medlemskapPerioder = new ArrayList<>();
    private Long fagsakId = nyId();
    private LocalDate behandlingstidFrist;
    private LocalDateTime opplysningerOppdatertTidspunkt;
    private String behandlendeEnhet;
    private BehandlingRepository mockBehandlingRepository;
    private BehandlingVedtak behandlingVedtak;
    private BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;

    // Registret og overstyrt personinfo
    private List<PersonInformasjon> personer;

    private Behandling originalBehandling;
    private BehandlingÅrsakType behandlingÅrsakType;
    private BehandlingRepositoryProvider repositoryProvider;
    private PersonInformasjon.Builder personInformasjonBuilder;
    private boolean manueltOpprettet;

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType) {
        this.fagsakBuilder = FagsakBuilder
            .nyFagsak(fagsakYtelseType)
            .medSaksnummer(new Saksnummer(nyId() + ""));
    }

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, AktørId aktørId) {
        this.fagsakBuilder = FagsakBuilder
            .nyFagsak(fagsakYtelseType)
            .medSaksnummer(new Saksnummer(nyId() + ""))
            .medBruker(new NavBrukerBuilder().medAktørId(aktørId).build());
    }

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, NavBruker navBruker) {
        this.fagsakBuilder = FagsakBuilder
            .nyFagsak(fagsakYtelseType)
            .medSaksnummer(new Saksnummer(nyId() + ""))
            .medBruker(navBruker);
    }

    static long nyId() {
        return FAKE_ID.getAndIncrement();
    }

    private BehandlingRepository lagBasicMockBehandlingRepository(BehandlingRepositoryProvider repositoryProvider) {
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);

        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);

        FagsakRepository mockFagsakRepository = mockFagsakRepository();
        PersonopplysningRepository mockPersonopplysningRepository = lagMockPersonopplysningRepository();
        MedlemskapRepository mockMedlemskapRepository = lagMockMedlemskapRepository();
        SøknadRepository søknadRepository = mockSøknadRepository();
        FagsakLåsRepository fagsakLåsRepository = mockFagsakLåsRepository();
        BehandlingsresultatRepository resultatRepository = mockBehandlingresultatRepository();
        VilkårResultatRepository vilkårResultatRepository = mockVilkårResultatRepository();

        BehandlingLåsRepository behandlingLåsReposiory = mockBehandlingLåsRepository();

        BehandlingVedtakRepository behandlingVedtakRepository = mockBehandlingVedtakRepository();
        // ikke ideelt å la mocks returnere mocks, men forenkler enormt mye test kode, forhindrer feil oppsett, så det
        // blir enklere å refactorere

        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getFagsakRepository()).thenReturn(mockFagsakRepository);
        when(repositoryProvider.getPersonopplysningRepository()).thenReturn(mockPersonopplysningRepository);
        when(repositoryProvider.getMedlemskapRepository()).thenReturn(mockMedlemskapRepository);
        when(repositoryProvider.getSøknadRepository()).thenReturn(søknadRepository);
        when(repositoryProvider.getBehandlingVedtakRepository()).thenReturn(behandlingVedtakRepository);
        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(fagsakLåsRepository);
        when(repositoryProvider.getBehandlingLåsRepository()).thenReturn(behandlingLåsReposiory);
        when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(resultatRepository);
        when(repositoryProvider.getVilkårResultatRepository()).thenReturn(vilkårResultatRepository);

        return behandlingRepository;
    }

    private VilkårResultatRepository mockVilkårResultatRepository() {
        return new VilkårResultatRepository() {
            private Map<Long, VilkårsResultat> entiteter = new HashMap<>();

            @Override
            public Optional<Vilkårene> hentHvisEksisterer(Long behandlingId) {
                return Optional.ofNullable(entiteter.get(behandlingId)).map(VilkårsResultat::getVilkårene);
            }

            @Override
            public void lagre(Long behandlingId, Vilkårene resultat) {
                entiteter.put(behandlingId, new VilkårsResultat(behandlingId, resultat));
            }

            @Override
            public Vilkårene hent(Long behandlingId) {
                return Optional.ofNullable(entiteter.get(behandlingId)).map(VilkårsResultat::getVilkårene).orElseThrow();
            }

            @Override
            public void kopier(Long fraBehandlingId, Long tilBehandlingId) {
                entiteter.put(fraBehandlingId, new VilkårsResultat(tilBehandlingId, hent(tilBehandlingId)));
            }
        };
    }

    private BehandlingsresultatRepository mockBehandlingresultatRepository() {
        return new BehandlingsresultatRepository() {
            @Override
            public Optional<Behandlingsresultat> hentHvisEksisterer(Long behandlingId) {
                Behandling behandling = behandlingMap.get(behandlingId);
                if (behandling == null) {
                    return Optional.empty();
                }
                return Optional.ofNullable(behandling.getBehandlingsresultat());
            }

            @Override
            public Behandlingsresultat hent(Long behandlingId) {
                Behandling behandling = behandlingMap.get(behandlingId);
                if (behandling == null) {
                    throw new IllegalStateException("Forventet behandlingsresultat");
                }
                return behandling.getBehandlingsresultat();
            }
        };
    }

    private BehandlingLåsRepository mockBehandlingLåsRepository() {
        return new BehandlingLåsRepository() {

            @Override
            public BehandlingLås taLås(Long behandlingId) {
                return new BehandlingLås(behandlingId);
            }

            @Override
            public void oppdaterLåsVersjon(BehandlingLås behandlingLås) {
            }

            @Override
            public BehandlingLås taLås(UUID eksternBehandlingRef) {
                return null;
            }
        };
    }

    private FagsakLåsRepository mockFagsakLåsRepository() {
        return new FagsakLåsRepository() {
            @Override
            public FagsakLås taLås(Long fagsakId) {
                return new FagsakLås(fagsakId) {

                };
            }

            @Override
            public FagsakLås taLås(Fagsak fagsak) {
                return new FagsakLås(fagsak.getId()) {

                };
            }

            @Override
            public void oppdaterLåsVersjon(FagsakLås fagsakLås) {

            }
        };
    }

    private BehandlingVedtakRepository mockBehandlingVedtakRepository() {
        BehandlingVedtakRepository behandlingVedtakRepository = mock(BehandlingVedtakRepository.class);
        BehandlingVedtak behandlingVedtak = mockBehandlingVedtak();
        when(behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(Mockito.any())).thenReturn(Optional.of(behandlingVedtak));

        return behandlingVedtakRepository;
    }

    public BehandlingVedtak mockBehandlingVedtak() {
        if (behandlingVedtak == null) {
            behandlingVedtak = Mockito.mock(BehandlingVedtak.class);
        }
        return behandlingVedtak;
    }

    private SøknadRepository mockSøknadRepository() {
        return new SøknadRepository() {

            private SøknadEntitet søknad;

            @Override
            public SøknadEntitet hentSøknad(Behandling behandling1) {
                return søknad;
            }

            @Override
            public Optional<SøknadEntitet> hentSøknadHvisEksisterer(Long behandlingId) {
                return Optional.ofNullable(søknad);
            }

            @Override
            public SøknadEntitet hentSøknad(Long behandlingId) {
                return søknad;
            }

            @Override
            public void lagreOgFlush(Behandling behandling, SøknadEntitet søknad1) {
                this.søknad = søknad1;
            }

        };
    }

    /**
     * Hjelpe metode for å håndtere mock repository.
     */
    public BehandlingRepository mockBehandlingRepository() {
        if (mockBehandlingRepository != null) {
            return mockBehandlingRepository;
        }
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        BehandlingRepository behandlingRepository = lagBasicMockBehandlingRepository(repositoryProvider);

        when(behandlingRepository.hentBehandling(Mockito.any(Long.class))).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return behandlingMap.getOrDefault(id, null);
        });
        when(behandlingRepository.hentBehandling(Mockito.any(UUID.class))).thenAnswer(a -> {
            throw new UnsupportedOperationException("Ikke implementert for AbstractTestScenario");
        });
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(Mockito.any())).thenAnswer(a -> {
            return List.copyOf(behandlingMap.values());
        });
        when(behandlingRepository.hentBehandlingHvisFinnes(Mockito.anyLong())).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return Optional.ofNullable(behandlingMap.getOrDefault(id, null));
        });
        when(behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(Mockito.any(), Mockito.any(BehandlingType.class)))
            .thenAnswer(a -> {
                Long id = a.getArgument(0);
                BehandlingType type = a.getArgument(1);
                return behandlingMap.values().stream().filter(b -> type.equals(b.getType()) && b.getFagsakId().equals(id)).sorted().findFirst();
            });
        when(behandlingRepository.hentSisteBehandlingForFagsakId(Mockito.any()))
            .thenAnswer(a -> {
                Long id = a.getArgument(0);
                return behandlingMap.values().stream().filter(b -> b.getFagsakId().equals(id)).sorted().findFirst();
            });
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(Mockito.any()))
            .thenAnswer(a -> {
                Long id = a.getArgument(0);
                return behandlingMap.values().stream()
                    .filter(b -> b.getFagsakId().equals(id) && b.getBehandlingsresultat() != null && !b.getBehandlingsresultat().isBehandlingHenlagt()).sorted()
                    .findFirst();
            });

        when(behandlingRepository.taSkriveLås(behandlingCaptor.capture())).thenAnswer((Answer<BehandlingLås>) invocation -> {
            Behandling beh = invocation.getArgument(0);
            return new BehandlingLås(beh.getId()) {
            };
        });

        when(behandlingRepository.hentSistOppdatertTidspunkt(Mockito.any()))
            .thenAnswer(a -> Optional.ofNullable(opplysningerOppdatertTidspunkt));

        when(behandlingRepository.lagre(behandlingCaptor.capture(), Mockito.any()))
            .thenAnswer((Answer<Long>) invocation -> {
                Behandling beh = invocation.getArgument(0);
                Long id = beh.getId();
                if (id == null) {
                    id = nyId();
                    Whitebox.setInternalState(beh, "id", id);
                }

                beh.getAksjonspunkter().forEach(punkt -> Whitebox.setInternalState(punkt, "id", nyId()));
                behandlingMap.put(id, beh);
                return id;
            });

        mockBehandlingRepository = behandlingRepository;
        return behandlingRepository;
    }

    public BehandlingRepositoryProvider mockBehandlingRepositoryProvider() {
        mockBehandlingRepository();
        return repositoryProvider;
    }

    private MedlemskapRepository lagMockMedlemskapRepository() {
        MedlemskapRepository dummy = new MedlemskapRepository(null) {
            @Override
            public void lagreOgFlush(Long behandlingId, Optional<MedlemskapBehandlingsgrunnlagEntitet> eksisterendeGrunnlag,
                                     MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag) {
                assert behandlingId != null : "behandlingId er null!";
                medlemskapgrunnlag.put(behandlingId, nyttGrunnlag);
            }

            @Override
            public void lagreMedlemskapRegistrert(MedlemskapRegistrertEntitet ny) {
                // ignore, tracker kun grunnlag for mock
            }

            @Override
            public void lagreOppgittTilknytning(MedlemskapOppgittTilknytningEntitet ny) {
                // ignore, tracker kun grunnlag for mock
            }

            @Override
            protected BehandlingLås taLås(Long behandlingId) {
                return null;
            }

            @Override
            protected void oppdaterLås(BehandlingLås lås) {
                // NO-OP i mock
            }

            @Override
            public void slettAvklarteMedlemskapsdata(Long behandlingId, BehandlingLås lås) {
                // NO-OP i mock
            }

            @Override
            protected Optional<MedlemskapBehandlingsgrunnlagEntitet> getAktivtBehandlingsgrunnlag(Long behandlingId) {
                assert behandlingId != null : "behandlingId er null!";
                return Optional.ofNullable(medlemskapgrunnlag.get(behandlingId));
            }
        };
        return Mockito.spy(dummy);
    }

    private PersonopplysningRepository lagMockPersonopplysningRepository() {
        return new MockPersonopplysningRepository();
    }

    private FagsakRepository mockFagsakRepository() {
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        when(fagsakRepository.finnEksaktFagsak(Mockito.anyLong())).thenAnswer(a -> fagsak);
        when(fagsakRepository.finnUnikFagsak(Mockito.anyLong())).thenAnswer(a -> Optional.of(fagsak));
        when(fagsakRepository.hentSakGittSaksnummer(Mockito.any(Saksnummer.class))).thenAnswer(a -> Optional.of(fagsak));
        when(fagsakRepository.hentForBruker(Mockito.any(AktørId.class))).thenAnswer(a -> singletonList(fagsak));
        when(fagsakRepository.opprettNy(fagsakCaptor.capture())).thenAnswer(invocation -> {
            Fagsak fagsak = invocation.getArgument(0); // NOSONAR
            Long id = fagsak.getId();
            if (id == null) {
                id = fagsakId;
                Whitebox.setInternalState(fagsak, "id", id);
            }
            return id;
        });

        // oppdater fagsakstatus
        Mockito.doAnswer(invocation -> {
            FagsakStatus status = invocation.getArgument(1);
            Whitebox.setInternalState(fagsak, "fagsakStatus", status);
            return null;
        }).when(fagsakRepository)
            .oppdaterFagsakStatus(eq(fagsakId), Mockito.any(FagsakStatus.class));

        return fagsakRepository;
    }

    public Fagsak lagreFagsak(BehandlingRepositoryProvider repositoryProvider) {
        lagFagsak(repositoryProvider.getFagsakRepository());
        return fagsak;
    }

    public Behandling lagre(BehandlingRepositoryProvider repositoryProvider) {

        build(repositoryProvider.getBehandlingRepository(), repositoryProvider);
        return behandling;
    }

    private BehandlingRepository lagMockedRepositoryForOpprettingAvBehandlingInternt() {
        if (mockBehandlingRepository != null && behandling != null) {
            return mockBehandlingRepository;
        }
        validerTilstandVedMocking();

        mockBehandlingRepository = mockBehandlingRepository();

        lagre(repositoryProvider); // NOSONAR //$NON-NLS-1$
        return mockBehandlingRepository;
    }

    public Behandling lagMocked() {
        lagMockedRepositoryForOpprettingAvBehandlingInternt();
        return behandling;
    }

    private void lagrePersonopplysning(BehandlingRepositoryProvider repositoryProvider, Behandling behandling) {
        PersonopplysningRepository personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        Long behandlingId = behandling.getId();

        if (personer != null && !personer.isEmpty()) {
            personer.stream().filter(e -> e.getType().equals(PersonopplysningVersjonType.REGISTRERT))
                .findFirst().ifPresent(e -> lagrePersoninfo(behandling, e, personopplysningRepository));

            personer.stream().filter(a -> a.getType().equals(PersonopplysningVersjonType.OVERSTYRT))
                .findFirst().ifPresent(b -> {
                if (personer.stream().noneMatch(c -> c.getType().equals(PersonopplysningVersjonType.REGISTRERT))) {
                    // Sjekker om overstyring er ok, mao om registeropplysninger finnes
                    personopplysningRepository.opprettBuilderForOverstyring(behandlingId);
                }
                lagrePersoninfo(behandling, b, personopplysningRepository);
            });

        } else {
            PersonInformasjon registerInformasjon = PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT)
                .leggTilPersonopplysninger(
                    no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning.builder()
                        .aktørId(behandling.getAktørId())
                        .navn("Forelder")
                        .fødselsdato(LocalDate.now().minusYears(25))
                        .sivilstand(SivilstandType.UOPPGITT)
                        .region(Region.NORDEN))
                .leggTilPersonstatus(Personstatus.builder()
                    .personstatus(PersonstatusType.BOSA)
                    .periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
                    .aktørId(behandling.getAktørId()))
                .build();
            lagrePersoninfo(behandling, registerInformasjon, personopplysningRepository);
        }
    }

    private void lagrePersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        Objects.nonNull(behandling);
        Objects.nonNull(personInformasjon);

        if (personInformasjon.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
            lagreRegisterPersoninfo(behandling, personInformasjon, repository);
        } else {
            lagreOverstyrtPersoninfo(behandling, personInformasjon, repository);
        }
    }

    private void lagreRegisterPersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        lagrePersoninfo(behandling, repository.opprettBuilderForRegisterdata(behandling.getId()), personInformasjon, repository);
    }

    private void lagreOverstyrtPersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        lagrePersoninfo(behandling, repository.opprettBuilderForOverstyring(behandling.getId()), personInformasjon, repository);
    }

    private void lagrePersoninfo(Behandling behandling, PersonInformasjonBuilder personInformasjonBuilder, PersonInformasjon personInformasjon,
                                 PersonopplysningRepository repository) {
        personInformasjon.getPersonopplysninger().forEach(e -> {
            PersonInformasjonBuilder.PersonopplysningBuilder builder = personInformasjonBuilder.getPersonopplysningBuilder(e.getAktørId());
            builder.medNavn(e.getNavn())
                .medFødselsdato(e.getFødselsdato())
                .medDødsdato(e.getDødsdato())
                .medKjønn(e.getBrukerKjønn())
                .medRegion(e.getRegion())
                .medSivilstand(e.getSivilstand());

            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getAdresser().forEach(e -> {
            PersonInformasjonBuilder.AdresseBuilder builder = personInformasjonBuilder.getAdresseBuilder(e.getAktørId(), e.getPeriode(), e.getAdresseType());
            builder.medAdresselinje1(e.getAdresselinje1())
                .medLand(e.getLand())
                .medPostnummer(e.getPostnummer());
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getPersonstatuser().forEach(e -> {
            PersonInformasjonBuilder.PersonstatusBuilder builder = personInformasjonBuilder.getPersonstatusBuilder(e.getAktørId(), e.getPeriode());
            builder.medPersonstatus(e.getPersonstatus());
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getStatsborgerskap().forEach(e -> {
            Region region = Region.finnHøyestRangertRegion(List.of(e.getStatsborgerskap().getKode()));
            PersonInformasjonBuilder.StatsborgerskapBuilder builder = personInformasjonBuilder.getStatsborgerskapBuilder(e.getAktørId(), e.getPeriode(),
                e.getStatsborgerskap(), region);
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getRelasjoner().forEach(e -> {
            PersonInformasjonBuilder.RelasjonBuilder builder = personInformasjonBuilder.getRelasjonBuilder(e.getAktørId(), e.getTilAktørId(),
                e.getRelasjonsrolle());
            builder.harSammeBosted(e.getHarSammeBosted());
            personInformasjonBuilder.leggTil(builder);
        });

        repository.lagre(behandling.getId(), personInformasjonBuilder);
    }

    private void validerTilstandVedMocking() {
        if (startSteg != null) {
            throw new IllegalArgumentException(
                "Kan ikke sette startSteg ved mocking siden dette krever Kodeverk.  Bruk ManipulerInternBehandling til å justere etterpå.");
        }
    }

    @SuppressWarnings("unchecked")
    public S medSøknadDato(LocalDate søknadsdato) {
        medSøknad().medSøknadsdato(søknadsdato);
        return (S) this;
    }

    private void build(BehandlingRepository behandlingRepo, BehandlingRepositoryProvider repositoryProvider) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        Builder behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();

        if (startSteg != null) {
            new InternalManipulerBehandling().forceOppdaterBehandlingSteg(behandling, startSteg);
        }

        leggTilAksjonspunkter(behandling);

        BehandlingLås lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);
        Long behandlingId = behandling.getId();

        lagrePersonopplysning(repositoryProvider, behandling);
        lagreMedlemskapOpplysninger(repositoryProvider, behandlingId);
        lagreSøknad(repositoryProvider);
        // opprett og lagre resulater på behandling
        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider, lås);

        if (this.opplysningerOppdatertTidspunkt != null) {
            behandlingRepo.oppdaterSistOppdatertTidspunkt(this.behandling, this.opplysningerOppdatertTidspunkt);
        }

        // få med behandlingsresultat etc.
        behandlingRepo.lagre(behandling, lås);
    }

    private void leggTilAksjonspunkter(Behandling behandling) {
        aksjonspunktDefinisjoner.forEach(
            (apDef, stegType) -> {
                if (stegType != null) {
                    new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling, apDef, stegType);
                } else {
                    new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling, apDef);
                }
            });
    }

    private void lagreSøknad(BehandlingRepositoryProvider repositoryProvider) {
        if (søknadBuilder != null) {
            final SøknadRepository søknadRepository = repositoryProvider.getSøknadRepository();
            søknadRepository.lagreOgFlush(behandling, søknadBuilder.build());
        }
    }

    private void lagreMedlemskapOpplysninger(BehandlingRepositoryProvider repositoryProvider, Long behandlingId) {
        repositoryProvider.getMedlemskapRepository().lagreMedlemskapRegisterOpplysninger(behandlingId, medlemskapPerioder);

        if (oppgittTilknytningBuilder != null) {
            final MedlemskapOppgittTilknytningEntitet oppgittTilknytning = medOppgittTilknytning().build();
            repositoryProvider.getMedlemskapRepository().lagreOppgittTilkytning(behandlingId, oppgittTilknytning);
        }
    }

    private Builder grunnBuild(BehandlingRepositoryProvider repositoryProvider) {
        FagsakRepository fagsakRepo = repositoryProvider.getFagsakRepository();

        lagFagsak(fagsakRepo);

        // oppprett og lagre behandling
        Builder behandlingBuilder;
        if (originalBehandling == null) {
            behandlingBuilder = Behandling.nyBehandlingFor(fagsak, behandlingType);
        } else {
            behandlingBuilder = Behandling.fraTidligereBehandling(originalBehandling, behandlingType)
                .medBehandlingÅrsak(
                    BehandlingÅrsak.builder(behandlingÅrsakType).medManueltOpprettet(manueltOpprettet).medOriginalBehandling(originalBehandling));
        }

        if (behandlingstidFrist != null) {
            behandlingBuilder.medBehandlingstidFrist(behandlingstidFrist);
        }

        if (behandlendeEnhet != null) {
            behandlingBuilder.medBehandlendeEnhet(new OrganisasjonsEnhet(behandlendeEnhet, null));
        }

        return behandlingBuilder;

    }

    private void lagFagsak(FagsakRepository fagsakRepo) {
        // opprett og lagre fagsak. Må gjøres før kan opprette behandling
        if (!Mockito.mockingDetails(fagsakRepo).isMock()) {
            final EntityManager entityManager = (EntityManager) Whitebox.getInternalState(fagsakRepo, "entityManager");
            if (entityManager != null) {
                BrukerTjeneste brukerTjeneste = new BrukerTjeneste(new NavBrukerRepository(entityManager));
                final Personinfo personinfo = new Personinfo.Builder()
                    .medFødselsdato(LocalDate.now())
                    .medPersonIdent(PersonIdent.fra("123451234123"))
                    .medNavn("asdf")
                    .medAktørId(fagsakBuilder.getBrukerBuilder().getAktørId())
                    .medForetrukketSpråk(
                        fagsakBuilder.getBrukerBuilder().getSpråkkode() != null ? fagsakBuilder.getBrukerBuilder().getSpråkkode() : Språkkode.nb)
                    .build();
                final NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktorId(personinfo);
                fagsakBuilder.medBruker(navBruker);
            }
        }
        fagsak = fagsakBuilder.build();
        Long fagsakId = fagsakRepo.opprettNy(fagsak); // NOSONAR //$NON-NLS-1$
        fagsak.setId(fagsakId);
    }

    private void lagreBehandlingsresultatOgVilkårResultat(BehandlingRepositoryProvider repoProvider, BehandlingLås lås) {
        // opprett og lagre behandlingsresultat med VilkårResultat og BehandlingVedtak
        Behandlingsresultat behandlingsresultat = (behandlingresultatBuilder == null ? Behandlingsresultat.builderForInngangsvilkår()
            : behandlingresultatBuilder).buildFor(behandling);
        behandlingresultatBuilder = null; // resett

        VilkårResultatBuilder inngangsvilkårBuilder = Vilkårene.builder();

        vilkårTyper.forEach((vilkårType, vilkårUtfallType) -> {
            inngangsvilkårBuilder.leggTil(new VilkårBuilder().medType(vilkårType).leggTil(new VilkårPeriodeBuilder()
                .medPeriode(LocalDate.now().minusMonths(3), LocalDate.now())
                .medUtfall(vilkårUtfallType)));
        });

        final var build = inngangsvilkårBuilder.build();

        repoProvider.getVilkårResultatRepository().lagre(behandling.getId(), build);

        if (behandlingVedtakBuilder != null) {
            // Må lagre Behandling for at Behandlingsresultat ikke skal være transient når BehandlingVedtak blir lagret:
            behandlingVedtak = behandlingVedtakBuilder.medBehandling(behandling.getId()).build();
            repoProvider.getBehandlingVedtakRepository().lagre(behandlingVedtak, lås);
        }
    }

    public Fagsak getFagsak() {
        if (fagsak == null) {
            throw new IllegalStateException("Kan ikke hente Fagsak før denne er bygd");
        }
        return fagsak;
    }

    public AktørId getDefaultBrukerAktørId() {
        return fagsakBuilder.getBrukerBuilder().getAktørId();
    }

    public Behandling getBehandling() {
        if (behandling == null) {
            throw new IllegalStateException("Kan ikke hente Behandling før denne er bygd");
        }
        return behandling;
    }

    @SuppressWarnings("unchecked")
    public S medSaksnummer(Saksnummer saksnummer) {
        fagsakBuilder.medSaksnummer(saksnummer);
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medFagsakId(Long id) {
        this.fagsakId = id;
        return (S) this;
    }

    public BehandlingVedtak.Builder medBehandlingVedtak() {
        if (behandlingVedtakBuilder == null) {
            behandlingVedtakBuilder = BehandlingVedtak.builder()
                // Setter defaultverdier
                .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
                .medAnsvarligSaksbehandler("Nav Navesen");
        }
        return behandlingVedtakBuilder;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingsresultat(Behandlingsresultat.Builder builder) {
        if (behandlingresultatBuilder == null) {
            behandlingresultatBuilder = builder;
        }
        return (S) this;
    }

    public MedlemskapOppgittTilknytningEntitet.Builder medOppgittTilknytning() {
        if (oppgittTilknytningBuilder == null) {
            oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder();
        }
        return oppgittTilknytningBuilder;
    }

    public MedlemskapOppgittTilknytningEntitet.Builder medDefaultOppgittTilknytning() {
        if (oppgittTilknytningBuilder == null) {
            oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder();
        }
        MedlemskapOppgittLandOppholdEntitet oppholdNorgeSistePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(true)
            .medLand(Landkoder.NOR)
            .medPeriode(
                LocalDate.now().minusYears(1),
                LocalDate.now())
            .build();
        MedlemskapOppgittLandOppholdEntitet oppholdNorgeNestePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.NOR)
            .medPeriode(
                LocalDate.now(),
                LocalDate.now().plusYears(1))
            .build();
        List<MedlemskapOppgittLandOppholdEntitet> oppholdNorge = List.of(oppholdNorgeNestePeriode, oppholdNorgeSistePeriode);

        oppgittTilknytningBuilder.medOpphold(oppholdNorge).medOppholdNå(true).medOppgittDato(LocalDate.now());
        return oppgittTilknytningBuilder;
    }

    public SøknadEntitet.Builder medSøknad() {
        if (søknadBuilder == null) {
            søknadBuilder = new SøknadEntitet.Builder();
        }
        return søknadBuilder;
    }

    protected void utenSøknad() {
        this.søknadBuilder = null;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingType(BehandlingType behandlingType) {
        this.behandlingType = behandlingType;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S leggTilVilkår(VilkårType vilkårType, Utfall utfall) {
        vilkårTyper.put(vilkårType, utfall);
        return (S) this;
    }

    public void leggTilAksjonspunkt(AksjonspunktDefinisjon apDef, BehandlingStegType stegType) {
        aksjonspunktDefinisjoner.put(apDef, stegType);
    }

    public void leggTilMedlemskapPeriode(MedlemskapPerioderEntitet medlemskapPeriode) {
        this.medlemskapPerioder.add(medlemskapPeriode);
    }

    @SuppressWarnings("unchecked")
    public S medBruker(AktørId aktørId) {
        fagsakBuilder
            .medBrukerAktørId(aktørId);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingStegStart(BehandlingStegType startSteg) {
        this.startSteg = startSteg;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingstidFrist(LocalDate behandlingstidFrist) {
        this.behandlingstidFrist = behandlingstidFrist;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlendeEnhet(String behandlendeEnhet) {
        this.behandlendeEnhet = behandlendeEnhet;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOpplysningerOppdatertTidspunkt(LocalDateTime opplysningerOppdatertTidspunkt) {
        this.opplysningerOppdatertTidspunkt = opplysningerOppdatertTidspunkt;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medRegisterOpplysninger(PersonInformasjon personinfo) {
        Objects.nonNull(personinfo);
        if (!personinfo.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
            throw new IllegalStateException("Feil versjontype, må være PersonopplysningVersjonType.REGISTRERT");
        }
        if (this.personer == null) {
            this.personer = new ArrayList<>();
            this.personer.add(personinfo);
        }
        return (S) this;
    }

    public PersonInformasjon.Builder opprettBuilderForRegisteropplysninger() {
        if (personInformasjonBuilder == null) {
            personInformasjonBuilder = PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT);
        }
        return personInformasjonBuilder;
    }

    public S medOriginalBehandling(Behandling originalBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        return medOriginalBehandling(originalBehandling, behandlingÅrsakType, false);
    }

    @SuppressWarnings("unchecked")
    private S medOriginalBehandling(Behandling originalBehandling, BehandlingÅrsakType behandlingÅrsakType, boolean manueltOpprettet) {
        this.originalBehandling = originalBehandling;
        this.behandlingÅrsakType = behandlingÅrsakType;
        this.manueltOpprettet = manueltOpprettet;
        return (S) this;
    }

    private final class MockPersonopplysningRepository extends PersonopplysningRepository {
        @Override
        public void kopierGrunnlagFraEksisterendeBehandlingForRevurdering(Long eksisterendeBehandlingId, Long nyBehandlingId) {
            final PersonopplysningGrunnlagBuilder oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                Optional.ofNullable(personopplysningMap.getOrDefault(eksisterendeBehandlingId, null)));

            personopplysningMap.put(nyBehandlingId, oppdatere.build());
        }

        @Override
        public Optional<PersonopplysningGrunnlagEntitet> hentPersonopplysningerHvisEksisterer(Long behandlingId) {
            return Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentPersonopplysninger(Long behandlingId) {
            if (personopplysningMap.isEmpty() || personopplysningMap.get(behandlingId) == null || !personopplysningMap.containsKey(behandlingId)) {
                throw new IllegalStateException("Fant ingen personopplysninger for angitt behandling");
            }

            return personopplysningMap.getOrDefault(behandlingId, null);
        }

        @Override
        public DiffResult diffResultat(PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2, boolean kunSporedeEndringer) {
            return null;
        }

        @Override
        public void lagre(Long behandlingId, PersonInformasjonBuilder builder) {
            final PersonopplysningGrunnlagBuilder oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null)));
            if (builder.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
                oppdatere.medRegistrertVersjon(builder);
            }
            if (builder.getType().equals(PersonopplysningVersjonType.OVERSTYRT)) {
                oppdatere.medOverstyrtVersjon(builder);
            }
            personopplysningMap.put(behandlingId, oppdatere.build());
        }

        @Override
        public PersonInformasjonBuilder opprettBuilderForOverstyring(Long behandlingId) {
            final Optional<PersonopplysningGrunnlagEntitet> grunnlag = Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
            return PersonInformasjonBuilder.oppdater(grunnlag.flatMap(PersonopplysningGrunnlagEntitet::getOverstyrtVersjon),
                PersonopplysningVersjonType.OVERSTYRT);
        }

        @Override
        public PersonInformasjonBuilder opprettBuilderForRegisterdata(Long behandlingId) {
            final Optional<PersonopplysningGrunnlagEntitet> grunnlag = Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
            return PersonInformasjonBuilder.oppdater(grunnlag.flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon),
                PersonopplysningVersjonType.REGISTRERT);
        }

        @Override
        public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
            final PersonopplysningGrunnlagBuilder oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                Optional.ofNullable(personopplysningMap.getOrDefault(gammelBehandlingId, null)));

            personopplysningMap.put(nyBehandlingId, oppdatere.build());
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentFørsteVersjonAvPersonopplysninger(Long behandlingId) {
            throw new java.lang.UnsupportedOperationException("Ikke implementert");
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentPersonopplysningerPåId(Long aggregatId) {
            throw new java.lang.UnsupportedOperationException("Ikke implementert");
        }
    }

}
