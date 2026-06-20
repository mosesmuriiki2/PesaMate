# PesaMate Feature Guide

## 🚀 Quick Start

Your PesaMate app now includes comprehensive financial management features. Here's what's new:

---

## 📱 Main Features

### 1. **Dashboard** (Home Screen)
**What's New:**
- ✅ Date range filter button (calendar icon)
- ✅ Refresh button to update data
- ✅ Visual loading indicator
- ✅ Shows filtered transactions based on date selection

**How to Use:**
1. Tap the calendar icon to select a date range
2. Tap refresh icon to sync latest SMS data
3. View your balance, income, and expenses for the selected period

---

### 2. **Loans Management** (New!)
**Features:**
- Track all your active loans
- See loan amounts, due dates, and interest rates
- View loan offers from SMS (Tala, Branch, M-Shwari, etc.)
- Add loans manually
- Get notifications for loans due soon

**How to Use:**
1. Navigate to "Loans" in bottom navigation
2. View all active loans with outstanding balances
3. Tap "+" button to add a manual loan
4. Loan offers from SMS appear automatically

**SMS Detection:**
- Automatically detects loan offers: "You qualify for a loan of KSh..."
- Tracks loan disbursements: "You have borrowed KSh..."
- Records repayments: "Loan repayment of KSh..."

---

### 3. **Insights** (Enhanced!)
**Features:**
- Income vs Expense comparison cards
- Average daily spending tracker
- Interactive pie chart by category
- Top spending categories
- Top merchants you pay most
- AI-powered insights and tips

**How to Use:**
1. Navigate to "Insights" tab
2. View visual breakdown of your spending
3. Check smart recommendations at the bottom
4. Track your savings rate

---

### 4. **Reports** (Enhanced!)
**Three Report Types:**

**a) Category Spending**
- Bar chart showing spending by category
- Detailed list with amounts
- Great for understanding where money goes

**b) Monthly Trend**
- Line chart comparing income vs expenses
- See patterns over time
- Identify months with high expenses

**c) Provider Breakdown**
- List of payment providers (M-Pesa, banks)
- Total spent through each provider
- Understand payment methods usage

**How to Use:**
1. Navigate to "Reports" tab
2. Select report type using buttons at top
3. Tap calendar icon to filter by date
4. Tap download icon to export (PDF coming soon)

---

### 5. **Settings** (Complete Redesign!)

#### Appearance
- **Dark Mode Toggle**: Switch between light and dark themes
- Theme saves automatically

#### Spending Limits
- **Set Daily Limit**: Configure maximum daily spending
- Get alerts at 75%, 90%, and 100% of limit
- Example: Set KSh 5,000 limit, get notified when you spend KSh 3,750 (75%)

#### Notifications
- **Push Notifications**: Enable/disable all notifications
- **Daily Limit Alerts**: Get spending threshold warnings
- **Loan Reminders**: Alerts for loans due soon

#### Security
- **Biometric Auth**: Fingerprint/Face unlock
- Secure your financial data

#### Data Management
- **Export Reports**: Download PDF reports
- **Cloud Backup**: Sync with Firebase
- **Manage Categories**: Customize transaction categories

**How to Set Daily Limit:**
1. Go to Settings → Spending Limits
2. Tap "Set" button
3. Enter amount (e.g., 5000)
4. Tap "Save"
5. You'll now get notifications when approaching limit

---

## 🔔 Notification System

### Types of Notifications:

**1. Spending Limit Alerts**
- 75% threshold: "Daily Spending Alert"
- 90% threshold: "⚠️ Daily Limit Almost Reached!"
- 100% threshold: "⚠️ Daily Limit Exceeded!"

**2. Loan Reminders**
- 7+ days before: "Loan Reminder"
- 3 days before: "Loan Due Soon!"
- Overdue: "Loan Payment Overdue!"

**3. Transaction Alerts**
- New transaction detected from SMS
- Sync completed

---

## 📊 Date Filtering

Available on all screens with this icon: 📅

**How to Use:**
1. Tap calendar icon
2. Select start date
3. Select end date
4. Tap "Apply"
5. All data updates to show only selected period

**Quick Tips:**
- Dashboard: Filter to see specific month's performance
- Reports: Compare different time periods
- Insights: Analyze spending patterns for any range

---

## 💰 Enhanced SMS Detection

### Now Detects:

**M-Pesa Transactions:**
- Money sent
- Money received
- Paybill payments
- Buy goods transactions

**Bank Transactions:**
- Equity Bank
- KCB Bank
- Co-operative Bank
- Absa Bank
- NCBA Bank
- Standard Chartered
- And more...

**Loan Services:**
- Tala loan offers & disbursements
- Branch loan offers & disbursements
- M-Shwari loans
- KCB M-Pesa loans
- Fuliza overdrafts
- Loan repayments

**SACCO & Savings:**
- SACCO contributions
- Chama contributions
- Savings deposits

---

## 🎨 UI Themes

### Light Mode (Default)
- Clean, bright interface
- Easy to read in daylight
- Professional look

### Dark Mode
- Reduces eye strain
- Saves battery (OLED screens)
- Modern aesthetic

**Switch Theme:**
Settings → Appearance → Dark Mode toggle

---

## 📈 Data Sync

### Automatic Sync
- Background sync every hour
- SMS detected in real-time
- Firebase cloud backup

### Manual Sync
- Tap refresh button on Dashboard
- Pull down on transaction lists
- Settings → Data Management → Cloud Backup

---

## 🔒 Security Features

1. **Biometric Authentication**: Lock app with fingerprint/face
2. **Encrypted Storage**: Secure local data storage
3. **Cloud Backup**: Optional Firebase sync
4. **Privacy**: Data stays on your device unless you enable cloud sync

---

## 💡 Pro Tips

1. **Set Realistic Limits**: Start with your average daily spending + 20%
2. **Review Insights Weekly**: Check top categories to find savings opportunities
3. **Use Date Filters**: Compare months to spot spending trends
4. **Track Loans Actively**: Add manual loans to see complete financial picture
5. **Enable Dark Mode**: Save battery and reduce eye strain at night

---

## 🆘 Troubleshooting

### "No data showing"
- Grant SMS permissions
- Tap refresh button
- Check date filter isn't too restrictive

### "Notifications not working"
- Check Settings → Notifications is enabled
- Verify Android notification permissions
- Ensure daily limit is set (for limit alerts)

### "SMS not detected"
- Ensure READ_SMS and RECEIVE_SMS permissions granted
- Check SMS is from supported provider
- Some SACCOs may have unique formats (can be added)

### "Dark mode not working"
- Close and reopen app
- Check Settings → Appearance → Dark Mode is ON

---

## 📞 Support

For issues or feature requests, check:
- App Version: Settings → About → App Version
- Current version: 1.0.0

---

**Enjoy your enhanced PesaMate experience!** 🎉
