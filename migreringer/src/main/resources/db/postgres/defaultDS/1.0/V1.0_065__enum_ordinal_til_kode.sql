update UTTALELSE_V2 set endring_type='ENDRET_INNTEKT' where endring_type = '0';
update UTTALELSE_V2 set endring_type='ENDRET_STARTDATO' where endring_type = '1';
update UTTALELSE_V2 set endring_type='ENDRET_SLUTTDATO' where endring_type = '2';
