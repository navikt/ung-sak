-- lukker feilet prosesstask etter kjøring av historiske vedtakhendelser TSF-1357
update prosess_task set status='KJOERT' where id in(
    12019841, 12018265, 12019845, 12019835, 12019619, 12018273) and status='FEILET';
