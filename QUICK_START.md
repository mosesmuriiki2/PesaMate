# PesaMate - Quick Start Guide

## 🚀 Build & Run (3 Steps)

```bash
# 1. Clean build
./gradlew clean

# 2. Build APK
./gradlew assembleDebug

# 3. Install on device
./gradlew installDebug
```

**That's it!** Your app is ready to use.

---

## 📱 First Launch Setup

1. **Grant Permissions**: Allow SMS reading when prompted
2. **Biometric Auth**: Set up fingerprint/face unlock
3. **Set Daily Limit**: Settings → Spending Limits → Set (e.g., 5000)
4. **Toggle Dark Mode**: Settings → Appearance → Dark Mode (optional)

---

## ✨ Key Features Overview

| Feature | Location | What It Does |
|---------|----------|--------------|
| 💰 Balance & Transactions | Dashboard | View net worth, filter by date |
| 💳 Loan Tracking | Loans Tab | Manage all loans & offers |
| 📊 Visual Analytics | Insights Tab | Pie charts, spending breakdown |
| 📈 Reports | Reports Tab | 3 report types with charts |
| ⚙️ Settings | Settings Tab | Dark mode, limits, notifications |

---

## 🔔 Notifications Setup

**Spending Alerts** (automatic when limit set):
- 75% spent → "Daily Spending Alert"
- 90% spent → "Almost at Limit!"
- 100% spent → "Limit Exceeded!"

**Enable in**: Settings → Notifications → Toggle ON

---

## 💡 Quick Tips

1. **Date Filtering**: Tap 📅 calendar icon on any screen
2. **Refresh Data**: Tap 🔄 refresh icon on Dashboard
3. **Add Loan**: Navigate to Loans → Tap + button
4. **Switch Theme**: Settings → Appearance → Dark Mode toggle
5. **View Reports**: Reports → Select Category/Monthly/Provider

---

## 🎨 UI Highlights

- **Material 3 Design**: Modern, clean interface
- **Dark Mode**: Auto status bar color
- **Smooth Animations**: Polished transitions
- **Card Layouts**: Easy to scan information
- **Color Coding**: Green (income), Red (expenses)

---

## 📋 Testing Checklist

Quick test after install:
- [ ] App opens
- [ ] Dashboard shows data
- [ ] Each tab opens
- [ ] Settings accessible
- [ ] Dark mode works
- [ ] Date filter works
- [ ] Notifications enabled

---

## 🐛 Troubleshooting

**No SMS detected?**
- Check READ_SMS permission granted
- Verify SMS format is supported

**No notifications?**
- Check Android notification settings
- Verify daily limit is set

**Theme not changing?**
- Close and reopen app
- Check Settings → Appearance

**Balance shows 0?**
- Tap refresh button
- Check date filter isn't too restrictive
- Wait for SMS background scan to complete

---

## 📖 Full Documentation

- **IMPLEMENTATION_SUMMARY.md** - Technical details
- **FEATURE_GUIDE.md** - Complete feature guide
- **BUILD_CHECKLIST.md** - Build & test checklist
- **BUILD_STATUS.md** - Current status report

---

## 🎯 Quick Feature Access

```
Dashboard  →  View balance, transactions, refresh data
Loans      →  Track loans, view offers, add manual loans
Insights   →  See pie charts, top categories, smart tips
Reports    →  Generate 3 types of financial reports
Settings   →  Configure app, set limits, toggle theme
```

---

## 💾 Data & Sync

**Local Storage**: Room Database (offline-first)  
**Cloud Backup**: Firebase Firestore (optional)  
**SMS Import**: Automatic on app launch  
**Sync Frequency**: Every hour (background)  
**Manual Sync**: Tap refresh button  

---

## 🔐 Security Features

✅ Biometric authentication (fingerprint/face)  
✅ Encrypted local storage  
✅ Optional cloud sync  
✅ Secure SMS reading  
✅ Privacy-first design  

---

## 🚀 You're All Set!

Your PesaMate app is now fully functional with:
- ✅ Loan tracking
- ✅ Smart notifications
- ✅ Beautiful insights
- ✅ Multiple reports
- ✅ Dark mode
- ✅ Date filtering
- ✅ Comprehensive settings

**Enjoy your enhanced financial management experience!** 🎉

---

**Version**: 1.0.0  
**Build Status**: ✅ Ready  
**Last Updated**: June 3, 2026
