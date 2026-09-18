update livsopphold_resultat_periode
set ikke_oppfylt_aarsak = 'MOTTAR_ANNEN_YTELSE'
where ikke_oppfylt_aarsak = 'HAR_ANNEN_LIVSOPPHOLDSYTELSE';
