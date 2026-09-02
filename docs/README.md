# README

## פרטי הקבוצה

| שם | תעודת זהות |
|---|---|
| זיו גלזר | 324273416 |
| דור גטניו | 211576863 |
| עידן קרייב | 211466610 |

## כלי מידול

- **draw.io** — תרשים Use Case ותרשים מחלקות (class diagram),תרשימי רצף (sequence diagrams)

## מבנה ההגשה

```
docs/
├── cases-use/
│   ├── use-case-diagram.pdf        # תרשים Use Case (סעיף 1.1)
│   ├── use-case-diagram.xml        # XML של תרשים Use Case
│   ├── use-case-e.pdf              # תרחיש e: יצירת הזמנה תקופתית מספק
│   ├── use-case-f.pdf              # תרחיש f: הוצאת הזמנה מספק עקב חוסר
│   ├── e-main.pdf                  # Activity Diagram — תרחיש e
│   └── f-main.pdf                  # Activity Diagram — תרחיש f
├── contracts/
│   ├── contract-addScheduledOrder.pdf     # חוזה 1: יצירת תבנית הזמנה תקופתית
│   ├── contract-addProductToOrder.pdf     # חוזה 2: הוספת מוצר להזמנה תקופתית
│   └── contract-addInventoryOrders.pdf    # חוזה 3: יצירת הזמנות רכש עקב חוסר
├── diagrams-sequence/
│   ├── seq-diagram-addScheduledOrder.pdf  # תרשים רצף — addScheduledOrder
│   ├── seq-diagram-addProductToOrder.pdf  # תרשים רצף — addProductToOrder
│   └── seq-diagram-addInventoryOrders.pdf # תרשים רצף — addInventoryOrders
├── class-diagram.pdf               # תרשים מחלקות מעודכן
├── class-diagram.xml               # XML תרשים מחלקות (draw.io)
├── requirements.pdf                # מסמך דרישות מעודכן
└── README.md                       # קובץ זה
