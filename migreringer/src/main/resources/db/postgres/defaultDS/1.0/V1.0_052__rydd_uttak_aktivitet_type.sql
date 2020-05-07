update ut_uttak_aktivitet_periode set aktivitet_type='AT' where aktivitet_type='0';
update ut_uttak_aktivitet_periode set aktivitet_type='SN' where aktivitet_type='1';
update ut_uttak_aktivitet_periode set aktivitet_type='FL' where aktivitet_type='2';
update ut_uttak_aktivitet_periode set aktivitet_type='ANNET' where aktivitet_type='3';
