package no.nav.ung.sak.formidling.pdfgen;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

class PdfGenObjectMapperConfig {

    static ObjectMapper lag() {
        var module = new SimpleModule();
        module.addSerializer(LocalDate.class, new NorskDatoSerialiserer());
        module.addSerializer(LocalDateTime.class, new NorskTidspunktSerialiserer());
        module.addSerializer(Double.class, new DesimaltallSerialiserer<>(Double.class));
        module.addSerializer(Float.class, new DesimaltallSerialiserer<>(Float.class));
        module.addSerializer(BigDecimal.class, new DesimaltallSerialiserer<>(BigDecimal.class));
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        return mapper;
    }
}

// NorskDatoSerialiserer
class NorskDatoSerialiserer extends StdSerializer<LocalDate> {

    private static final DateTimeFormatter NORSK_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.of("no", "NO"));

    public NorskDatoSerialiserer() {
        super(LocalDate.class);
    }

    @Override
    public void serialize(LocalDate dato, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(dato.format(NORSK_DATE_FORMATTER));
    }
}

// NorskTidspunktSerialiserer
class NorskTidspunktSerialiserer extends StdSerializer<LocalDateTime> {

    private static final DateTimeFormatter NORSK_DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("d. MMMM yyyy 'kl.' HH:mm", Locale.of("no", "NO"));

    public NorskTidspunktSerialiserer() {
        super(LocalDateTime.class);
    }

    @Override
    public void serialize(LocalDateTime tidspunkt, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(tidspunkt.format(NORSK_DATETIME_FORMATTER));
    }
}

// DesimaltallSerialiserer
class DesimaltallSerialiserer<T extends Number> extends StdSerializer<T> {

    public DesimaltallSerialiserer(Class<T> clazz) {
        super(clazz);
    }

    @Override
    public void serialize(T desimaltall, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (desimaltall == null) {
            return;
        }

        int heltall = desimaltall.intValue();
        if (Double.compare(desimaltall.doubleValue(), heltall) == 0) {
            gen.writeNumber(heltall);
        } else {
            // Rounding to Float to avoid precision errors if Float -> Double
            gen.writeNumber(desimaltall.floatValue());
        }
    }
}
