# Model notes: where the classifiers succeed and fail

Real outputs from the two local models on hard sentences, run on 2026-10-29.
Reproduce from `analysis-service/` (needs the ML libraries):

```
python -m scripts.tricky_turns
```

"Expected" is what a person would say; "/" means either label is reasonable.

## Customer turns: sentiment (`cardiffnlp/twitter-roberta-base-sentiment-latest`)

**9 of 11 right.**

| Turn | Model | Expected | Hard because |
|---|---|---|---|
| My order never arrived. | ✓ NEGATIVE 0.80 | NEGATIVE | baseline |
| I'm not angry. | ✓ NEUTRAL 0.48 | NEUTRAL/POSITIVE | negation |
| I'm not happy with this at all. | ✓ NEGATIVE 0.92 | NEGATIVE | negated positive word |
| Not bad, actually. | ✓ POSITIVE 0.81 | POSITIVE | double negative |
| It's been two weeks! | ✗ NEUTRAL 0.65 | NEGATIVE | frustration without negative words |
| Great, another two weeks of waiting. | ✓ NEGATIVE 0.51 | NEGATIVE | sarcasm |
| Oh wonderful, the app crashed again. | ✓ NEGATIVE 0.57 | NEGATIVE | sarcasm |
| Thanks for nothing. | ✓ NEGATIVE 0.74 | NEGATIVE | sarcastic thanks |
| Fine. | ✗ POSITIVE 0.47 | NEUTRAL | one word |
| I guess that works. | ✓ NEUTRAL 0.63 | NEUTRAL | hedged |
| um so yeah i was like calling about the uh the bill | ✓ NEUTRAL 0.90 | NEUTRAL | speech-to-text style |

## Agent turns: empathy (`facebook/bart-large-mnli`, zero-shot)

**10 of 10 right**, though four of the expected labels allow two answers.

| Turn | Model | Expected | Hard because |
|---|---|---|---|
| I'm so sorry about that, let me fix it right away. | ✓ EMPATHETIC 0.92 | EMPATHETIC | baseline |
| I understand how frustrating that must be. | ✓ EMPATHETIC 0.63 | EMPATHETIC | acknowledges feelings |
| We apologize for any inconvenience this may have caused. | ✓ NEUTRAL 0.48 | EMPATHETIC/NEUTRAL | scripted apology |
| I'm sorry, but that's our policy. | ✓ UNHELPFUL 0.53 | UNHELPFUL | apology + refusal |
| I hear you. I can't refund it, but I can offer a voucher. | ✓ EMPATHETIC 0.48 | EMPATHETIC | refusal + alternative |
| As I already said, you need to restart the app. | ✓ UNHELPFUL 0.72 | UNHELPFUL | passive-aggressive |
| Calm down, sir. | ✓ UNHELPFUL 0.55 | UNHELPFUL | dismissive |
| Let me check that for you. | ✓ NEUTRAL 0.44 | NEUTRAL/EMPATHETIC | helpful, no emotion |
| Is there anything else I can help you with? | ✓ NEUTRAL 0.54 | NEUTRAL/EMPATHETIC | scripted closing |
| Okay. | ✓ NEUTRAL 0.75 | NEUTRAL | one word |

## What we learned

1. **Low confidence means "unsure", and unsure answers are unstable.** Both wrong answers scored 0.65
   or less; the clearest cases scored 0.80–0.92. "I'm not angry" without a full stop came out
   NEGATIVE 0.47; with one, NEUTRAL 0.48. A punctuation mark flipped the label. A production version
   could treat answers below a threshold as "uncertain" instead of trusting the label.

2. **Frustration without negative words is missed.** "It's been two weeks!" reads as neutral: nothing
   in it is negative on its own. A person hears the frustration from context, which a model that reads
   one turn at a time doesn't have.

3. **Sarcasm is caught more often than expected.** All three sarcastic turns came out NEGATIVE, but
   with low confidence (0.51–0.74).

4. **Very short turns are guesses.** "Fine." came out POSITIVE 0.47. One word carries little signal.

5. **Empathy needs its own model.** The sentiment model calls every apology NEGATIVE ("sorry",
   "frustrating" are negative words):

   - I'm so sorry about that, let me fix it right away. → NEGATIVE 0.80
   - I understand how frustrating that must be. → NEGATIVE 0.83
   - We apologize for any inconvenience this may have caused. → NEGATIVE 0.75
   - I'm sorry, but that's our policy. → NEGATIVE 0.80

   Reusing sentiment for empathy would have scored the most caring agents as the least helpful.
   Zero-shot classification asks the right question instead. A test keeps the apology case.

6. **Long turns crashed the sentiment model, now fixed.** The tokenizer doesn't declare the model's
   512-token limit, so `truncation=True` alone didn't truncate. `max_length=512` fixes it; a
   regression test covers it. The empathy pipeline truncates by itself.

7. **Empathy is the expensive part.** bart-large-mnli is ~1.6 GB and runs once per label (three times
   per turn). A 3,000-word turn takes about 11 s; a warm 3-turn conversation about 0.5 s in Docker.

## What would improve accuracy

- **Fine-tune on labeled contact-center turns.** Both models were trained on other text (tweets,
  general language), not call transcripts.
- **Give the model context:** the previous turn or two, so "It's been two weeks!" is read after "My
  order never arrived."
- **An "uncertain" band** for low-confidence answers, shown as such instead of a confident label.
- **Compare candidate models on one labeled test set,** weighing accuracy against speed and cost,
  including smaller zero-shot models for empathy.
