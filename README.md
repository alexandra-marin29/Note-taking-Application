# Note-taking-Application


NoteApp is an Android application that allows users to create, edit, and organize notes within folders. Users can choose between standard text notes and checklist notes. The app offers a range of functionalities including:

- **Adding new notes:** Create text-based notes or checklists.
- **Attaching images and links:** Easily add photos from your device and include useful URLs.
- **Customizable note colors:** Change the background color of your notes for a personalized experience.
- **Reminders:** Set precise reminders for your notes using system notifications.
- **Pinning notes:** Keep important notes at the top of your list.
  
The app is built using the MVVM architecture and leverages:
- **Room** for local database storage and serialization of notes and folders.
- **Firebase Authentication** for secure user login (email/password and Google Sign-In).
- **Firebase Firestore** for synchronizing data across devices.
- **Retrofit** for HTTP calls to fetch motivational quotes.
- **AlarmManager & NotificationCompat** for scheduling and displaying native system notifications.

With a responsive design that adapts well in both portrait and landscape modes, NoteApp ensures a smooth user experience while maintaining robust data handling and synchronization.
