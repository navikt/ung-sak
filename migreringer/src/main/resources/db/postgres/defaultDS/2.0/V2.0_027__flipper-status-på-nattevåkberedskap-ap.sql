UPDATE aksjonspunkt
set aksjonspunkt_status = 'UTFO'
where aksjonspunkt_status = 'AVBR'
  and aksjonspunkt_def in ('9200', '9201');
