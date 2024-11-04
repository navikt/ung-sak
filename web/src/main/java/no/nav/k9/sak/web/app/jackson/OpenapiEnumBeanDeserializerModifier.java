package no.nav.k9.sak.web.app.jackson;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.EnumResolver;

/**
 * Modifiserer deserialisering for enums til å først bruke @JsonValue basert deserialisering, deretter toString()
 * viss ingen @JsonValue annotasjon finnast på enum som skal deserialiserast.
 * <p>
 * For at denne skal fungere som tenkt må den brukast i ObjectMapper der OpenapiCompatAnnotationIntrospector er aktivert,
 * slik at evt @JsonCreator, @JsonFormat og @JsonSerialize er ignorert ved serialisering og deserialisering.
 * <p>
 * ObjectMapper skal og ha SerializationFeature.WRITE_ENUMS_USING_TO_STRING aktivert, men DeserializationFeature.READ_ENUMS_USING_TO_STRING skal <b>ikkje</b> vere aktivert.
 */
public class OpenapiEnumBeanDeserializerModifier extends BeanDeserializerModifier {

    // Basert på implementasjon frå jackson.databind.deser.BasicDeserializerFactory.
    protected EnumResolver constructPrimaryEnumResolver(final DeserializationConfig config, final BeanDescription beanDesc) {
        final AnnotatedMember jvAcc = beanDesc.findJsonValueAccessor();
        if (jvAcc != null) {
            if (config.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(jvAcc.getMember(),
                    config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
            return EnumResolver.constructUsingMethod(config, beanDesc.getClassInfo(), jvAcc);
        }
        return EnumResolver.constructUsingToString(config, beanDesc.getClassInfo());
    }

    @Override
    public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        return new EnumDeserializer(
            constructPrimaryEnumResolver(config, beanDesc),
            config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS),
            null,
            EnumResolver.constructUsingToString(config, beanDesc.getClassInfo())
        );
    }


}
