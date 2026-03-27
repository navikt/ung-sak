---
name: inntektskontroll
description: Explains the domain of income control (inntektskontroll) in the context of the ung stack and provides guidance on how to approach it
---

# Inntektskontroll Domain expert
The youth program allowance (ungdomsprogramytelsen) is paid out monthly. The applicant's monthly payment is reduced by 66% of the income they have had in the same month as they attended the youth program. Income control (inntektskontroll) is the process of verifying the income information provided by applicants for benefits in the ung stack.

# Forhåndsvarsel (notification of discrepancy)
Whenever the system wants to use information which is not provided by the applicant and differs from what the applicant has reported, the applicant is notified of this and given the opportunity to provide a statement regarding the discrepancy.
This is done to ensure that the applicants have the opportunity to explain any discrepancies in their reporting.

# Register information retrieval (registerinnhenting)
The process of income control involves checking the income against income information from a-ordningen, to ensure that the payment is reduced in case the applicant has had income in the same month as they attended the youth program (ungdomsprogrammet).
Income information from a-ordningen is retrieved through an external application called inntektskomponenten and stored in k9-abakus.
The income information is available through InntektArbeidYtelseAggregat in InntektArbeidYtelseGrunnlag (IAYGrunnlag) in the ung-sak application which is retrieved from k9-abakus after the process of register information retrieval (registerinnhenting) is completed.

# The process of income control in the ung stack
At the start of each month, the applicants are notified to report their income for the previous month. They can report their income through an assignment (oppgave) triggered by ung-sak via `OpprettOppgaverForInntektsrapporteringBatchTask` and handled by ung-brukerdialog-api.
On the 8th of each month, the income control process starts. Ung-sak runs the scheduled task `OpprettRevurderingForInntektskontrollBatchTask` which starts the process of income control for all relevant cases (fagsaker).
This creates a new `Behandling` (of type revurdering) for relevant `Fagsak` and this revurdering is flagged for income control by creating a `ProsessTrigger` with the `BehandlingÅrsakType` `RE-KONTROLL-REGISTER-INNTEKT`.

The income control process checks the reported income against the income information from a-ordningen. Determining the right income is a two step process:
1) Creating a notification of discrepancy (forhåndsvarsel) in case of a discrepancy between the income from a-ordningen and the reported income from the applicant
2) Determining the right income

## Notification of discrepancy for income control
The process of determining whether to create a notification of discrepancy (forhåndsvarsel) is based on the comparison between the income from a-ordningen and the reported income from the applicant. 
This is done in `EtterlysningutlederKontrollerInntekt` which is called from `VurderKompletthetSteg`. This can result in an `Etterlysning` which is the application's way of representing the need to retrieve more information, and in this case, the need to create a notification of discrepancy (forhåndsvarsel) for the applicant.
The logic for determining whether to create a notification of discrepancy (forhåndsvarsel) is as follows:
- If income from a-ordningen and the reported income from the applicant are approximately the same (smaller than 15 NOK difference) , the payment is reduced by 66% of the reported income from the applicant and there is no need for a notification of discrepancy (forhåndsvarsel) or a statement from the applicant.
- If income from a-ordningen and the reported income from the applicant are not the same, and the income from a-ordningen is not zero, the applicant is notified (varsel) of this discrepancy (avvik).

The applicant can then choose to accept the income information from a-ordningen, or they can choose to provide a statement (uttalelse) regarding the discrepancy between the reported income and the income from a-ordningen.

## Determining the right income
This process is handled in `KontrollerInntektSteg`.
In case there is no (or up to 15 NOK) discrepancy between the income from a-ordningen and the reported income from the applicant, the correct income is determined to be the reported income from the applicant and the payment is reduced by 66% of the reported income from the applicant.
If the applicant was notified of a discrepancy (avvik) between the income from a-ordningen and the reported income from the applicant, but accepted the discrepancy and did not provide a statement, the correct income is determined to be the income from a-ordningen and the payment is reduced by 66% of the income from a-ordningen.
If the applicant provides a statement, the case is flagged for manual review by a caseworker (aksjonspunkt). It is up to the caseworker to decide whether to accept the income information from a-ordningen, or to accept the applicants statement and reduce the payment by 66% of the reported income from the applicant.
Alternatively, the caseworker can determine a different income based on the information provided by the applicant and the income from a-ordningen.

In case where the income from a-ordningen is zero and the reported income from the applicant is above zero the case is also flagged for manual review by a caseworker (aksjonspunkt).
In this case the caseworker is asked to determine the correct income and possibly contact the applicant for more information. It is likely that the applicant has misunderstood the income reporting.
