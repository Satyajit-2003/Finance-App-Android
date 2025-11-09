# 📱 SpendTrackr – Personal Expense Logger

## 📋 Setup Instructions

### Prerequisites

- [ ] Android device (Android 6.0 or higher)
- [ ] Backend API deployed and running ([Setup Guide](https://github.com/Satyajit-2003/Finance-backend-API))
- [ ] API Key from your backend deployment

### Step 1: Deploy Backend API

1. **Follow the complete setup guide** in the [Finance Backend API repository](https://github.com/Satyajit-2003/Finance-backend-API)
2. **Deploy to Render** using the free tier (detailed instructions provided)
3. **Note down**:
   - Your API URL: `https://your-service-name.onrender.com`
   - Your API Key from the backend configuration

### Step 2: Install Android App

1. **Download the latest APK** from the [Releases page](../../releases)
2. **Enable "Install from Unknown Sources"** if prompted
3. **Install the APK** on your Android device

### Step 3: Configure App Settings

1. **Open SpendTrackr** on your device
2. **Go to Settings tab**
3. **Enter your backend details**:
   ```
   API URL: https://your-service-name.onrender.com
   API Key: your_secure_api_key_here
   ```
4. **Test Connection** using the button in settings
5. **Grant SMS permissions** when prompted

### Step 4: Start Tracking!

- The app will automatically detect and log transaction SMS
- All data syncs to your Google Sheet in real-time
- Use the app to view, edit, and categorize your expenses

---

## 📌 How to Use

1. **Install the app** and grant SMS read permission.
2. When you receive a **transaction SMS**, it's automatically logged.
3. Open the app to:
   - View today's transactions.
   - Tap on the **date** to pick another day.
   - Tap the **edit icon** to modify entries.
4. Use the **Chart tab** to visualize category-wise spending.
5. All entries sync to your **Google Sheet** in real time.

SpendTrackr is your lightweight companion for **tracking personal expenses**, **splits with friends**, and **categorizing spending**, all synced seamlessly with Google Sheets.

> 🔗 **Backend API**: This app requires the [Finance Backend API](https://github.com/Satyajit-2003/Finance-backend-API) to be deployed. Follow the complete setup guide in the backend repository.

---

## 🚀 Features

- ✅ **Automatic SMS Parsing**: Detects and logs transactions from SMS alerts.
- 📊 **Charts & Summaries**: Visualize your spending across categories.
- 🗂️ **Smart Categorization**: Color-coded categories for better clarity.
- ✏️ **Edit & Delete**: Modify transactions, update notes, or delete entries with ease.
- 📅 **Date Navigation**: Select any date to view or manage transactions for that day.
- 🔗 **Google Sheets Sync**: All transactions are logged to your personal sheet.
- 🔄 **Pull to Refresh**: Swipe down to fetch latest entries manually.

---

## 🧾 What You Can Track

- Daily transactions
- Friend splits (with 50:50 toggle)
- Notes for any entry
- Detailed pie charts per month

---

## 📌 How to Use

1. **Install the app** and grant SMS read permission.
2. When you receive a **transaction SMS**, it’s automatically logged.
3. Open the app to:
   - View today’s transactions.
   - Tap on the **date** to pick another day.
   - Tap the **edit icon** to modify entries.
4. Use the **Chart tab** to visualize category-wise spending.
5. All entries sync to your **Google Sheet** in real time.

---

## 🛠️ Notes

- Only SMSes with supported formats are logged.
- App uses a **color-coded card layout** for intuitive browsing.
- If a friend owes you a part of the amount, just enter it under **Friend Split**.
- **Free Tier Note**: Backend may sleep after 15 minutes of inactivity - first request may take 30 seconds.

---

## 🔗 Related Links

- **[Backend API Repository](https://github.com/Satyajit-2003/Finance-backend-API)** - Complete setup guide for the backend
- **[API Documentation](https://github.com/Satyajit-2003/Finance-backend-API/blob/main/endpoints.md)** - Technical API reference
- **[Deployment Guide](https://github.com/Satyajit-2003/Finance-backend-API/blob/main/README.md)** - Step-by-step deployment to Render

---

## 🆘 Troubleshooting

### App Can't Connect to Backend

- Verify API URL is correct (include `https://`)
- Check API Key matches your backend configuration
- Test backend health: `https://your-service-name.onrender.com/health`
- If using free tier, wait 30 seconds for backend to wake up

### SMS Not Being Detected

- Ensure SMS read permission is granted
- Check if your bank's SMS format is supported
- Manually test with the backend API if needed

### Transactions Not Syncing

- Check internet connection
- Verify Google Sheets permissions are set correctly
- Review backend logs for any errors

---

## 🧯 Privacy

SpendTrackr reads your SMS locally. No SMS data is stored outside your device except for the processed transactions you explicitly log to your Google Sheet.
