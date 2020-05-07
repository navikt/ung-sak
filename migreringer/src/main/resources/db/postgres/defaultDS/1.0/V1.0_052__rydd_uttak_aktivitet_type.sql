update ut_uttak_aktivitet_periode set aktivitet_type='ARBEIDSTAKER' where aktivitet_type='0';
update ut_uttak_aktivitet_periode set aktivitet_type='SELVSTENDIG_NÆRINGSDRIVENDE' where aktivitet_type='1';
update ut_uttak_aktivitet_periode set aktivitet_type='FRILANSER' where aktivitet_type='2';
update ut_uttak_aktivitet_periode set aktivitet_type='ANNET' where aktivitet_type='3';
