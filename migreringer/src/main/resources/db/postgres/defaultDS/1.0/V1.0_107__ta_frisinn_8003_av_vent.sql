UPDATE aksjonspunkt set frist_tid = current_date - interval '1 day' WHERE aksjonspunkt_def = '8003' AND aksjonspunkt_status = 'OPPR';
