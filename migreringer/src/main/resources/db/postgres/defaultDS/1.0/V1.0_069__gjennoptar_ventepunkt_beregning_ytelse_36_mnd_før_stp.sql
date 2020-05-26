UPDATE aksjonspunkt set frist_tid = current_date WHERE aksjonspunkt_def = '8000' AND aksjonspunkt_status = 'OPPR' AND vent_aarsak = 'INGEN_PERIODE_UTEN_YTELSE';
