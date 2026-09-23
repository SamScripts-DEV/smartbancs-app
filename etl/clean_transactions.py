import pandas as pd

INPUT_FILE = "raw_transactions_sample.csv"
OUTPUT_FILE = "clean_transactions_output.csv"


def clean_amount(value):
    return float(str(value).replace("$", "").replace(",", "").strip())


def normalize_date(value):
    text = str(value).strip()
    for fmt in ("%d/%m/%Y", "%Y-%m-%d"):
        try:
            return pd.to_datetime(text, format=fmt).strftime("%Y-%m-%d")
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text}")


def normalize_account_number(value):
    return str(value).strip().upper()


def main():
    df = pd.read_csv(INPUT_FILE)
    total_rows = len(df)

    missing_description = df["description"].isna() | (df["description"].astype(str).str.strip() == "")
    missing_count = int(missing_description.sum())

    df["amount"] = df["amount"].apply(clean_amount)
    df["transaction_date"] = df["transaction_date"].apply(normalize_date)
    df["account_number"] = df["account_number"].apply(normalize_account_number)
    df.loc[missing_description, "description"] = "No description provided"
    df["description"] = df["description"].astype(str).str.strip()

    df.to_csv(OUTPUT_FILE, index=False)

    print(f"Rows processed: {total_rows}")
    print(f"Missing descriptions found and filled: {missing_count}")
    print(f"Output written to: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
