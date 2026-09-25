package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttAndreLivsoppholdsytelser;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttAndreLivsoppholdsytelser.Livsoppholdsårsak;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBistand.Bistandsårsak;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted.Bostedsårsak;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvslåttVilkårBrevinnholdHjelperTest {

    private static final String FRITEKST = "Saksbehandlers begrunnelse.";

    // AVKORTET er kun et teknisk avslag, som ikke brevet kal begrunne, og UDEFINERT er ingen årsak.
    private static final Set<String> IKKE_I_BREVET = Set.of("AVKORTET", "UDEFINERT");

    @DisplayName("Hver bostedsårsak i kodeverket oversettes til sin egen årsak i brevet")
    @Test
    void bostedsårsakerOversettesEntydig() {
        var brevårsaker = oversett(BostedsvilkårIkkeOppfyltÅrsak.class, VilkårType.BOSTEDSVILKÅR,
            vurdering -> AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(vurdering).årsak());

        assertThat(brevårsaker).doesNotHaveDuplicates().containsExactlyInAnyOrder(Bostedsårsak.values());
    }

    @DisplayName("Hver bistandsårsak i kodeverket oversettes til sin egen årsak i brevet")
    @Test
    void bistandsårsakerOversettesEntydig() {
        var brevårsaker = oversett(BistandsvilkårIkkeOppfyltÅrsak.class, VilkårType.BISTANDSVILKÅR,
            vurdering -> AvslåttVilkårBrevinnholdHjelper.lagAvslåttBistand(vurdering).årsak());

        assertThat(brevårsaker).doesNotHaveDuplicates().containsExactlyInAnyOrder(Bistandsårsak.values());
    }

    @DisplayName("Hver livsoppholdsårsak i kodeverket oversettes til sin egen årsak i brevet")
    @Test
    void livsoppholdsårsakerOversettesEntydig() {
        var brevårsaker = oversett(AndreLivsoppholdsytelserIkkeOppfyltÅrsak.class, VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR,
            vurdering -> AvslåttVilkårBrevinnholdHjelper.lagAvslåttPgaAndreLivsoppholdsytelser(vurdering).årsak());

        assertThat(brevårsaker).doesNotHaveDuplicates().containsExactlyInAnyOrder(Livsoppholdsårsak.values());
    }

    @DisplayName("Alle livsoppholdsårsaker utenom MOTTAR_ANNEN_YTELSE navngir ytelsen i brevet")
    @Test
    void livsoppholdsårsakerNavngirYtelsen() {
        for (var årsak : EnumSet.complementOf(EnumSet.of(Livsoppholdsårsak.MOTTAR_ANNEN_YTELSE))) {
            assertThat(AvslåttAndreLivsoppholdsytelser.av(årsak, FRITEKST).ytelseNavn()).isNotBlank();
        }
        assertThat(AvslåttAndreLivsoppholdsytelser.av(Livsoppholdsårsak.MOTTAR_ANNEN_YTELSE, FRITEKST).ytelseNavn()).isNull();
    }

    @DisplayName("Fritekst følger med standardårsaken, den erstatter den ikke")
    @Test
    void fritekstKommerITilleggTilStandardårsaken() {
        var livsopphold = AvslåttVilkårBrevinnholdHjelper.lagAvslåttPgaAndreLivsoppholdsytelser(
            vurdering(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER, FRITEKST));

        assertThat(livsopphold.årsak()).isEqualTo(Livsoppholdsårsak.MOTTAR_DAGPENGER);
        assertThat(livsopphold.ytelseNavn()).isEqualTo("dagpenger");
        assertThat(livsopphold.fritekstBrev()).isEqualTo(FRITEKST);
    }

    @DisplayName("Årsak som ikke krever fritekst kan stå uten")
    @Test
    void fritekstErValgfriNårÅrsakenIkkeKreverDen() {
        var bosted = AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(
            vurdering(VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, null));

        assertThat(bosted.årsak()).isEqualTo(Bostedsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);
        assertThat(bosted.fritekstBrev()).isNull();
    }

    @DisplayName("Årsak som krever fritekst feiler uten")
    @Test
    void fritekstErPåkrevdNårÅrsakenKreverDen() {
        var vurdering = vurdering(VilkårType.BISTANDSVILKÅR, BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, null);

        assertThatThrownBy(() -> AvslåttVilkårBrevinnholdHjelper.lagAvslåttBistand(vurdering))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("IKKE_14A_VEDTAK");
    }

    @DisplayName("Årsak fra et annet vilkår feiler")
    @Test
    void årsakFraFeilVilkårFeiler() {
        var vurdering = vurdering(VilkårType.BOSTEDSVILKÅR, BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, FRITEKST);

        assertThatThrownBy(() -> AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(vurdering))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ukjent ikkeOppfyltÅrsak");
    }

    @DisplayName("AVKORTET og UDEFINERT skal være filtrert bort før brevet bygges")
    @Test
    void årsakUtenBrevtekstFeiler() {
        var avkortet = vurdering(VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.AVKORTET, FRITEKST);

        assertThatThrownBy(() -> AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(avkortet))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AVKORTET");
    }

    private static <K extends Enum<K> & IkkeOppfyltDetaljertÅrsak, B> List<B> oversett(Class<K> kodeverk,
                                                                                      VilkårType vilkårType,
                                                                                      Function<VilkårsvurderingResultat, B> tilBrevårsak) {
        return Arrays.stream(kodeverk.getEnumConstants())
            .filter(årsak -> !IKKE_I_BREVET.contains(årsak.name()))
            .map(årsak -> tilBrevårsak.apply(vurdering(vilkårType, årsak, FRITEKST)))
            .toList();
    }

    private static VilkårsvurderingResultat vurdering(VilkårType vilkårType, IkkeOppfyltDetaljertÅrsak årsak, String fritekstBrev) {
        return new VilkårsvurderingResultat(vilkårType, false, årsak, true, "begrunnelse", fritekstBrev, "Z123456", null);
    }
}
