package no.nav.k9.kodeverk;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.EnumResolver;

import java.util.List;

public class OpenapiEnumBeanDeserializerModifier extends BeanDeserializerModifier {

    @Override
    public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config, BeanDescription beanDesc, List<BeanPropertyDefinition> propDefs) {
        return super.updateProperties(config, beanDesc, propDefs);
    }

    @Override
    public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder) {
        return super.updateBuilder(config, beanDesc, builder);
    }

    // Copied in from
    protected EnumResolver constructPrimaryEnumResolver(Class<?> enumClass, DeserializationConfig config, BeanDescription beanDesc) {
        AnnotatedMember jvAcc = beanDesc.findJsonValueAccessor();
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
        final var resolved = super.modifyEnumDeserializer(config, type, beanDesc, deserializer);
        if(!(resolved instanceof EnumDeserializer)) {
            // Vi har fått ein "creator based deserializer". Det fungerer ikkje for openapi serialiserte enums, så
            // returner vanleg EnumDeserializer istaden, oppsatt til å bruke kun @JsonValue eller toString() for
            // deserialisering.
            return new EnumDeserializer(
                constructPrimaryEnumResolver(type.getRawClass(), config, beanDesc),
                config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS),
                null,
                EnumResolver.constructUsingToString(config, beanDesc.getClassInfo())
            );
        }
        return resolved;
    }


}
