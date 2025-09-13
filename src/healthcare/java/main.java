package healthcare;
import healthcare.model.*;
import healthcare.exceptions.*;
import healthcare.utils.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
//Main program for my healthcare system assignment
//This is the console interface part that handles all user interactions
//I'm implementing advanced features to make it more impressive
//Created by: Aluka Sai Medha

public class main {
    private static final Scanner keyboardInput = new Scanner(System.in);
    private static HospitalSystem mainHospitalSystem;
    private static LoginManager userLoginSystem;
    private static SmartMenuHandler menuHandler;
    private static SystemWatcher performanceWatcher;
    // Making the console look professional with these
    private static final String FANCY_LINE = "═".repeat(60);
    private static final String SIMPLE_LINE = "─".repeat(45);
    private static final String CHECK_MARK = "Correct";
    private static final String X_MARK = "Wrong";
    private static final String WARNING_SIGN = "Warning";
    private static final String INFO_BUBBLE = "Information";
    public static void main(String[] args) {
        showWelcomeScreen();
        try {
            startupAllSystems();
            runSystemDiagnostics();
            runMainProgram();
        } catch (MajorSystemProblem bigProblem) {
            handleBigProblems(bigProblem);
        } catch (Exception randomError) {
            handleWeirdErrors(randomError);
        } finally {
            shutDownEverything();
        }
    }
    //Shows a nice welcome screen when program starts
    private static void showWelcomeScreen() {
        System.out.println(FANCY_LINE);
        System.out.println("RMIT HEALTHCARE SYSTEM");
        System.out.println("Welcome to HealthCare");
        System.out.println("Built: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println(FANCY_LINE);
    }
    //Gets  all the different parts of my system running
    private static void startupAllSystems() throws MajorSystemProblem {
        try {
            System.out.println(INFO_BUBBLE + " Loading the main hospital system...");
            mainHospitalSystem = new HospitalSystem("RMIT Healthcare Center");

            System.out.println(INFO_BUBBLE + " Setting up user login stuff...");
            userLoginSystem = new LoginManager(mainHospitalSystem);

            System.out.println(INFO_BUBBLE + " Creating smart menu system...");
            menuHandler = new SmartMenuHandler(keyboardInput);

            System.out.println(INFO_BUBBLE + " Starting performance monitoring...");
            performanceWatcher = new SystemWatcher();

            // Tries to load any saved data from before
            mainHospitalSystem.loadOldData();

            System.out.println(CHECK_MARK + " Everything loaded successfully! Ready to go!");

        } catch (Exception startupProblem) {
            throw new MajorSystemProblem("Couldn't start the system properly",
                    "STARTUP_FAILED", startupProblem);
        }
    }
    //Checks if everything is working properly before we start
    private static void runSystemDiagnostics() {
        System.out.println(INFO_BUBBLE + "Doing some quick checks...");
        CheckResult memoryOkay = performanceWatcher.checkIfMemoryIsOkay();
        CheckResult dataLooksGood = mainHospitalSystem.checkIfDataIsValid();
        CheckResult rulesAreFollowed = mainHospitalSystem.quickComplianceCheck();
        if (memoryOkay.isGood() && dataLooksGood.isGood() && rulesAreFollowed.isGood()) {
            System.out.println(CHECK_MARK + "All checks passed,we're good to go!");
        } else {
            System.out.println(WARNING_SIGN + "we found some minor issues but we can still run");
        }
    }
    //The main loop where everything happens
    private static void runMainProgram() {
        boolean keepProgramRunning = true;
        UserSession currentUser = null;
        while (keepProgramRunning) {
            try {
                if (currentUser == null || !currentUser.isStillActive()) {
                    currentUser = tryToLoginUser();
                    if (currentUser == null) {
                        continue; // Login failed, try again
                    }
                }
                WhatUserWants userChoice = showMainMenuAndGetChoice(currentUser);
                keepProgramRunning = doWhatUserWants(userChoice, currentUser);
            } catch (SessionTimeoutException timeoutProblem) {
                System.out.println(WARNING_SIGN + " Your session has been timed out. Please login again.");
                currentUser = null;
            } catch (BadInputException inputProblem) {
                System.out.println(X_MARK + " That input doesn't work: " + inputProblem.getMessage());
                System.out.println(INFO_BUBBLE + " Try again with something that makes sense.");
            }
        }
    }
    //Handles the user login with some security
    private static UserSession tryToLoginUser() {
        System.out.println("\n" + SIMPLE_LINE);
        System.out.println("YOU NEED TO LOGIN FIRST");
        System.out.println(SIMPLE_LINE);
        int loginTries = 0;
        final int maxTries = 3;
        while (loginTries < maxTries) {
            try {
                String username = menuHandler.askForText("Username", TextChecker.LETTERS_AND_NUMBERS);
                String password = menuHandler.askForPassword("Password");
                LoginResult loginCheck = userLoginSystem.tryLogin(username, password);
                if (loginCheck.workedOkay()) {
                    System.out.println(CHECK_MARK + " Login worked! Hi there, " + loginCheck.getUserType());
                    return new UserSession(loginCheck.getLoggedInUser(), LocalDateTime.now().plusHours(8));
                } else {
                    loginTries++;
                    System.out.println(X_MARK + " Login failed: " + loginCheck.getWhyItFailed());
                    System.out.println(WARNING_SIGN + " You have " + (maxTries - loginTries) + " tries left");
                }
            } catch (BadInputException inputProblem) {
                System.out.println(X_MARK + " That input format is wrong: " + inputProblem.getMessage());
                loginTries++;
            }
        }
        System.out.println(X_MARK + " Too many failed attempts. Access denied!");
        return null;
    }
    //Shows the main menu based on who's logged in
    private static WhatUserWants showMainMenuAndGetChoice(UserSession currentUser) throws BadInputException {
        System.out.println("\n" + FANCY_LINE);
        System.out.println("HEALTHCARE MANAGEMENT DASHBOARD");
        System.out.println("User: " + currentUser.getWhoIsLoggedIn().getFullName());
        System.out.println("Role: " + currentUser.getWhoIsLoggedIn().getWhatTheyCanDo());
        System.out.println("Time left: " + currentUser.getTimeRemaining() + " minutes");
        System.out.println(FANCY_LINE);
        List<MenuChoice> whatTheyCanDo = makeMenuBasedOnRole(currentUser.getWhoIsLoggedIn().getWhatTheyCanDo());
        for (int i = 0; i < whatTheyCanDo.size(); i++) {
            MenuChoice option = whatTheyCanDo.get(i);
            System.out.println((i + 1) + ". " + option.getIcon() + " " + option.getDescription());
        }
        System.out.println((whatTheyCanDo.size() + 1) + "Logout");
        System.out.println((whatTheyCanDo.size() + 2) + "Save and Quit");
        int whatTheyPicked = menuHandler.askForNumberInRange("Pick an option", 1, whatTheyCanDo.size() + 2) - 1;
        if (whatTheyPicked == whatTheyCanDo.size()) {
            return new WhatUserWants(UserAction.LOGOUT);
        } else if (whatTheyPicked == whatTheyCanDo.size() + 1) {
            return new WhatUserWants(UserAction.QUIT_PROGRAM);
        } else {
            return new WhatUserWants(whatTheyCanDo.get(whatTheyPicked).getWhatItDoes());
        }
    }
    //Actually do what the user selected
    private static boolean doWhatUserWants(WhatUserWants userChoice, UserSession currentUser) {
        try {
            switch (userChoice.getWhatToDo()) {
                case HANDLE_STAFF_STUFF -> dealWithStaffManagement(currentUser);
                case HANDLE_PATIENT_STUFF -> dealWithPatientCare(currentUser);
                case WATCH_FACILITY -> monitorFacilityOperations(currentUser);
                case CHECK_COMPLIANCE -> analyzeComplianceStuff(currentUser);
                case MAKE_REPORTS -> generateSomeReports(currentUser);
                case CHANGE_SETTINGS -> configureSystemStuff(currentUser);
                case LOGOUT -> {
                    currentUser.endSession();
                    System.out.println(CHECK_MARK + "Logged out successfully");
                    return true;
                }
                case QUIT_PROGRAM -> {
                    saveEverythingAndQuit();
                    return false;
                }
            }
        } catch (NotAllowedException permissionProblem) {
            System.out.println(X_MARK + " You are not allowed to do that: " + permissionProblem.getMessage());
        } catch (OperationFailedException operationProblem) {
            System.out.println(X_MARK + " That didn't work: " + operationProblem.getMessage());
            System.out.println(INFO_BUBBLE + " System is still running fine, try something else");
        }
        return true;
    }
    //Handles all the staff management stuff
    private static void dealWithStaffManagement(UserSession currentUser) throws OperationFailedException {
        System.out.println("\n" + SIMPLE_LINE);
        System.out.println("STAFF MANAGEMENT SECTION");
        System.out.println(SIMPLE_LINE);
        List<StaffOption> staffChoices = Arrays.asList(
                new StaffOption("Add New Doctor", StaffTask.ADD_DOCTOR),
                new StaffOption("Add New Nurse", StaffTask.ADD_NURSE),
                new StaffOption("Set Up Work Schedules", StaffTask.MANAGE_SCHEDULES),
                new StaffOption("Check Staff Performance", StaffTask.CHECK_PERFORMANCE),
                new StaffOption("Generate Staff Reports", StaffTask.MAKE_REPORTS)
        );
        StaffOption whatTheyWant = menuHandler.showChoicesAndGetPick(staffChoices);
        switch (whatTheyWant.getTask()) {
            case ADD_DOCTOR -> addNewDoctor(currentUser);
            case ADD_NURSE -> addNewNurse(currentUser);
            case MANAGE_SCHEDULES -> setupWorkSchedules(currentUser);
            case CHECK_PERFORMANCE -> checkStaffPerformance(currentUser);
            case MAKE_REPORTS -> makeStaffReports(currentUser);
        }
    }
    //Add a new doctor with proper validation
    private static void addNewDoctor(UserSession currentUser) throws OperationFailedException {
        System.out.println("\n ADDING NEW DOCTOR");
        try {
            DoctorInfo newDoctorData = new DoctorInfo.Builder()
                    .setStaffID(menuHandler.askForText("Doctor ID", TextChecker.STAFF_ID_FORMAT))
                    .setFullName(menuHandler.askForText("Full Name", TextChecker.PERSON_NAME_FORMAT))
                    .setEmail(menuHandler.askForText("Email", TextChecker.EMAIL_FORMAT))
                    .setPhone(menuHandler.askForText("Phone", TextChecker.PHONE_FORMAT))
                    .setLoginStuff(
                            menuHandler.askForText("Username", TextChecker.USERNAME_FORMAT),
                            menuHandler.askForPassword("Password")
                    )
                    .setMedicalStuff(menuHandler.askForText("Medical Specialty", TextChecker.SPECIALTY_FORMAT))
                    .setLicense(menuHandler.askForText("License Number", TextChecker.LICENSE_FORMAT))
                    .build();
            Doctor newDoctor = mainHospitalSystem.addNewDoctor(newDoctorData);
            System.out.println(CHECK_MARK + " Dr. " + newDoctor.getFullName() + " added successfully!");
            System.out.println(INFO_BUBBLE + " Doctor ID: " + newDoctor.getStaffID());
            System.out.println(INFO_BUBBLE + " Specialty: " + newDoctor.getMedicalSpecialty());
        } catch (BadDataException dataError) {
            throw new OperationFailedException("Couldn't add doctor - data problem", dataError);
        } catch (AlreadyExistsException duplicateError) {
            throw new OperationFailedException("That staff ID already exists", duplicateError);
        }
    }
    //Handles patient care operations
    private static void dealWithPatientCare(UserSession currentUser) throws OperationFailedException {
        System.out.println("\n" + SIMPLE_LINE);
        System.out.println("PATIENT CARE MANAGEMENT");
        System.out.println(SIMPLE_LINE);
        List<PatientOption> patientChoices = Arrays.asList(
                new PatientOption("Smart Patient Admission", PatientTask.SMART_ADMIT),
                new PatientOption("Move Patients Around", PatientTask.REARRANGE_BEDS),
                new PatientOption("Check Patient Status", PatientTask.CHECK_PATIENTS),
                new PatientOption("Discharge Planning", PatientTask.PLAN_DISCHARGE),
                new PatientOption("Monitor Facility", PatientTask.WATCH_FACILITY)
        );
        PatientOption whatTheyPicked = menuHandler.showChoicesAndGetPick(patientChoices);
        switch (whatTheyPicked.getTask()) {
            case SMART_ADMIT -> doSmartPatientAdmission(currentUser);
            case REARRANGE_BEDS -> rearrangeBedAssignments(currentUser);
            case CHECK_PATIENTS -> checkOnPatients(currentUser);
            case PLAN_DISCHARGE -> planPatientDischarges(currentUser);
            case WATCH_FACILITY -> watchFacilityRealTime(currentUser);
        }
    }
    //Smart patient admission using AI-like logic
    private static void doSmartPatientAdmission(UserSession currentUser) throws OperationFailedException {
        System.out.println("\n SMART ADMISSION SYSTEM");
        try {
            PatientDetails patientInfo = collectPatientInformation();
            //It finds algorithm
            BedFinder bedFinder = mainHospitalSystem.getBedFinder();
            BestBedSuggestion suggestion = bedFinder.findBestBed(patientInfo);
            if (suggestion.foundSomething()) {
                System.out.println(INFO_BUBBLE + " Smart Recommendation: " + suggestion.getWhy());
                System.out.println(INFO_BUBBLE + " Best Bed: " + suggestion.getBestBed().getBedID());
                System.out.println(INFO_BUBBLE + " How sure: " + suggestion.getHowSure() + "%");
                boolean userLikesIt = menuHandler.askYesOrNo("Use this recommendation?");
                if (userLikesIt) {
                    Patient newPatient = mainHospitalSystem.admitPatientToBed(
                            patientInfo, suggestion);
                    System.out.println(CHECK_MARK + "Patient " + newPatient.getFullName() +
                            " admitted to " + newPatient.getCurrentBed().getBedID());
                } else {
                    // Lets them pick manually
                    pickBedManually(patientInfo);
                }
            } else {
                System.out.println(WARNING_SIGN + " No beds available right now. Adding to waiting list.");
                mainHospitalSystem.addToWaitingList(patientInfo);
            }
        } catch (PatientRegistrationProblem registrationError) {
            throw new OperationFailedException("Patient admission failed", registrationError);
        }
    }
    //Collects the patient information with validation
    private static PatientDetails collectPatientInformation() throws BadInputException {
        System.out.println("Patient Information");
        return new PatientDetails.Builder()
                .setPatientID(menuHandler.askForText("Patient ID", TextChecker.PATIENT_ID_FORMAT))
                .setFullName(menuHandler.askForText("Full Name", TextChecker.PERSON_NAME_FORMAT))
                .setContactInfo(
                        menuHandler.askForText("Email", TextChecker.EMAIL_FORMAT),
                        menuHandler.askForText("Phone", TextChecker.PHONE_FORMAT)
                )
                .setPersonalInfo(
                        menuHandler.askForGender(),
                        menuHandler.askForDate("Date of Birth")
                )
                .setMedicalInfo(
                        menuHandler.askForText("Main Condition", TextChecker.MEDICAL_CONDITION_FORMAT),
                        menuHandler.askYesOrNo("Needs Isolation?"),
                        menuHandler.askForText("Allergies", TextChecker.MEDICAL_TEXT_FORMAT),
                        menuHandler.askForText("Current Meds", TextChecker.MEDICAL_TEXT_FORMAT)
                )
                .setCareNeeds(
                        menuHandler.askForCareLevel(),
                        menuHandler.askForMobilityLevel(),
                        menuHandler.askForDietNeeds()
                )
                .build();
    }
    //Advanced compliance checking with predictions
    private static void analyzeComplianceStuff(UserSession currentUser) throws OperationFailedException {
        System.out.println("\n COMPLIANCE ANALYSIS");
        try {
            ComplianceChecker checker = mainHospitalSystem.getComplianceChecker();
            // Does a full compliance check
            FullComplianceReport report = checker.doFullAnalysis();
            System.out.println("COMPLIANCE DASHBOARD");
            System.out.println(SIMPLE_LINE);
            showComplianceNumbers(report);
            showRiskStuff(report.getRiskAnalysis());
            showSuggestions(report.getSuggestions());
            // Predicts the future compliance issues
            if (menuHandler.askYesOrNo("Want to see future predictions?")) {
                FutureCompliancePrediction prediction = checker.predictFutureIssues();
                showFuturePredictions(prediction);
            }
        } catch (ComplianceAnalysisProblem analysisError) {
            throw new OperationFailedException("Compliance analysis failed", analysisError);
        }
    }
    //Shows compliance metrics in a nice format
    private static void showComplianceNumbers(FullComplianceReport report) {
        System.out.println("🔍 Current Status:");
        System.out.println("  Staff Levels: " + formatPercentage(report.getStaffingLevel()));
        System.out.println("  Shift Coverage: " + formatPercentage(report.getShiftCoverage()));
        System.out.println("  Bed Usage: " + formatPercentage(report.getBedUsage()));
        System.out.println("  Safety Rules: " + formatPercentage(report.getSafetyCompliance()));
        System.out.println("  Paperwork: " + formatPercentage(report.getDocumentationLevel()));
    }
    //Saves everything and quit properly
    private static void saveEverythingAndQuit() {
        System.out.println("\n" + SIMPLE_LINE);
        System.out.println("SAVING AND SHUTTING DOWN");
        System.out.println(SIMPLE_LINE);
        try {
            System.out.println(INFO_BUBBLE + " Saving all data...");
            mainHospitalSystem.saveEverything();
            System.out.println(INFO_BUBBLE + " Writing shutdown log...");
            mainHospitalSystem.logShutdown();
            System.out.println(INFO_BUBBLE + " Checking data integrity...");
            boolean dataIsOkay = mainHospitalSystem.checkDataBeforeShutdown();
            if (dataIsOkay) {
                System.out.println(CHECK_MARK + " Shutdown completed successfully!");
                System.out.println("Thanks for using my healthcare system!");
            } else {
                System.out.println(WARNING_SIGN + " Found some data issues during shutdown");
                System.out.println(INFO_BUBBLE + " You might want to check with IT");
            }
        } catch (Exception shutdownProblem) {
            System.out.println(X_MARK + " Problem during shutdown: " + shutdownProblem.getMessage());
            System.out.println(WARNING_SIGN + "Emergency shutdown mode activated");
        }
    }
    // It helps methods that make things look nice
    private static String formatPercentage(double percentage) {
        if (percentage >= 95) return CHECK_MARK + " " + String.format("%.1f%%", percentage);
        if (percentage >= 80) return WARNING_SIGN + " " + String.format("%.1f%%", percentage);
        return X_MARK + " " + String.format("%.1f%%", percentage);
    }
    private static List<MenuChoice> makeMenuBasedOnRole(UserRole role) {
        List<MenuChoice> options = new ArrayList<>();
        options.add(new MenuChoice("Handle Staff Stuff", UserAction.HANDLE_STAFF_STUFF));
        options.add(new MenuChoice("Handle Patient Stuff", UserAction.HANDLE_PATIENT_STUFF));
        options.add(new MenuChoice("Watch Facility", UserAction.WATCH_FACILITY));
        if (role.canDoAdvancedStuff()) {
            options.add(new MenuChoice("Check Compliance", UserAction.CHECK_COMPLIANCE));
            options.add(new MenuChoice("Make Reports", UserAction.MAKE_REPORTS));
        }
        if (role.canChangeSystemStuff()) {
            options.add(new MenuChoice("Change Settings", UserAction.CHANGE_SETTINGS));
        }
        return options;
    }
    private static void handleBigProblems(MajorSystemProblem error) {
        System.err.println("\n" + X_MARK + " MAJOR SYSTEM PROBLEM");
        System.err.println("Problem Type: " + error.getErrorType());
        System.err.println("What happened: " + error.getMessage());
        System.err.println("Shutting down to prevent data corruption...");
    }
    private static void handleWeirdErrors(Exception error) {
        System.err.println("\n" + X_MARK + " UNEXPECTED ERROR");
        System.err.println("Error: " + error.getMessage());
        System.err.println("Please tell someone in IT about this error.");
        error.printStackTrace();
    }
    private static void shutDownEverything() {
        if (keyboardInput != null) {
            keyboardInput.close();
        }
        System.out.println("\n All done! Goodbye!");
    }
}