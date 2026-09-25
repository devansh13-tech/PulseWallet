# Datasets

Nothing in this directory is committed. `.gitignore` excludes `data/raw/`,
`data/processed/` and `*.csv` deliberately — the Kaggle credit-card fraud file
is roughly 144 MB and GitHub rejects any single file over 100 MB.

Anyone setting up the project downloads the data themselves using the steps below.

## Layout

```text
data/
├── raw/          # downloaded datasets, never modified
└── processed/    # train/test splits and engineered features produced by scripts
```

Treat `raw/` as read-only. Every transformation should be a script that reads from
`raw/` and writes to `processed/`, so the pipeline is reproducible from a fresh
download. A preprocessing step you performed by hand once, in a notebook, is a
step you cannot defend in a viva.

## Credit Card Fraud Detection dataset (Milestone 4)

Source: <https://www.kaggle.com/datasets/mlg-ulb/creditcardfraud>

Manual download: get `creditcard.csv` from the page above and place it at
`data/raw/creditcard.csv`.

Or via the Kaggle CLI:

```bash
pip install kaggle
# Put your kaggle.json API token in ~/.kaggle/ first
kaggle datasets download -d mlg-ulb/creditcardfraud -p data/raw --unzip
```

Expected shape: 284,807 rows, 31 columns, 492 fraud cases (about 0.172%).

### Two things to know before training

**The class imbalance is extreme.** At 0.172% positives, a model that predicts
"never fraud" is 99.83% accurate and useless. Report precision, recall, PR-AUC
and the confusion matrix. Accuracy alone is the single easiest thing for an
examiner to attack.

**Apply SMOTE after the train/test split, to the training fold only.** Oversampling
the full dataset before splitting copies synthetic neighbours of test rows into
the training set. The resulting scores look excellent and mean nothing. Use an
`imblearn.pipeline.Pipeline` so this cannot happen by accident.

### The feature-mismatch problem

Columns `V1`–`V28` are anonymised PCA components. They do not correspond to
anything FlexGuard records about a transaction (amount, category, timestamp,
merchant, user history), and there is no way to derive them. A model trained on
these features **cannot** score a real transaction from this application.

Decide how to handle this before Week 6 — `docs/ROADMAP.md` lays out three
options under Milestone 4. Leaving it until integration in Week 9 is how the two
halves of this project end up unable to talk to each other.
