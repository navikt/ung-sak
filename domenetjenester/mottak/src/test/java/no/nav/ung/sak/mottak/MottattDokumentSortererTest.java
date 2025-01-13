package no.nav.ung.sak.mottak;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;

public class MottattDokumentSortererTest {


    @Test
    public void skal_sortere_etter_mottatt_dag_når_den_er_ulik() {
        // Arrange
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder
            .medMottattTidspunkt(LocalDateTime.now())
            .medFagsakId(41337L);

        MottattDokument første = builder.build();

        MottattDokument.Builder builder2 = new MottattDokument.Builder();
        builder2.medMottattTidspunkt(LocalDateTime.now().plusDays(1))
            .medFagsakId(41337L);
        MottattDokument andre = builder2.build();

        List<MottattDokument> dokumenter = List.of(andre, første);

        // Act
        List<MottattDokument> sortert = dokumenter.stream().sorted(MottattDokumentSorterer.sorterMottattDokument()).collect(Collectors.toList());

        // Assert
        Assertions.assertThat(sortert).containsExactly(første, andre);
    }

}
