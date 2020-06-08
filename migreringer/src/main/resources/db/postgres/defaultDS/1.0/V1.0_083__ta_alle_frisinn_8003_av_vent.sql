-- Ta av førstegangsbehandlinger av vent
UPDATE aksjonspunkt
set frist_tid = current_date - interval '1 day'
WHERE aksjonspunkt_def = '8003'
  AND aksjonspunkt_status = 'OPPR'
  AND vent_aarsak != 'FRISINN_VARIANT_ENDRET_INNTEKTSTYPE'; -- Kun annengangssøknad får denne venteårsaken
