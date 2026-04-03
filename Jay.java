private List<CSVRecord> getRevalCPUsers(List<String> revalResponseList) {
    try {
        // Parse headers
        String[] revalUserReportHeader = getHeaders(revalResponseList.get(0));
        String[] syfUserRolesHeader = getHeaders(revalResponseList.get(1));
        String[] revalCpUserRolesHeader = getHeaders(revalResponseList.get(2));

        // Stream parse instead of getRecords() to avoid loading everything at once
        List<CSVRecord> revalUserReportRecords;
        List<CSVRecord> syfUserRolesRecords;
        List<CSVRecord> revalCpUserRolesRecords;

        try (CSVParser revalUserReportParser = CSVParser.parse(revalResponseList.get(0),
                CSVFormat.DEFAULT.withHeader(revalUserReportHeader));
             CSVParser syfUserRolesParser = CSVParser.parse(revalResponseList.get(1),
                CSVFormat.DEFAULT.withHeader(syfUserRolesHeader));
             CSVParser revalCpUserRolesParser = CSVParser.parse(revalResponseList.get(2),
                CSVFormat.DEFAULT.withHeader(revalCpUserRolesHeader))) {

            // Materialize only once, but you could also stream directly if you don’t need all records
            revalUserReportRecords = new ArrayList<>();
            for (CSVRecord record : revalUserReportParser) {
                revalUserReportRecords.add(record);
            }

            syfUserRolesRecords = new ArrayList<>();
            for (CSVRecord record : syfUserRolesParser) {
                syfUserRolesRecords.add(record);
            }

            revalCpUserRolesRecords = new ArrayList<>();
            for (CSVRecord record : revalCpUserRolesParser) {
                revalCpUserRolesRecords.add(record);
            }
        }

        logger.info("Total Records of reval user beginning= {}", revalUserReportRecords.size());

        // Build sets/maps for O(1) lookups instead of repeated stream scans
        Set<String> revalUserReportSet = revalUserReportRecords.stream()
                .map(record -> record.get(RevalServiceConstants.USERNAME2))
                .collect(Collectors.toSet());

        Map<String, CSVRecord> cpUserMap = revalCpUserRolesRecords.stream()
                .collect(Collectors.toMap(r -> r.get("User"), r -> r, (r1, r2) -> r1));

        // Find CP users not in revalUserReportSet
        List<CSVRecord> cpUserRecords = cpUserMap.entrySet().stream()
                .filter(entry -> !revalUserReportSet.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        logger.info("Total Records of CP user= {}", cpUserRecords.size());

        CSVRecord revalUserHeaderRecord = revalUserReportRecords.get(0);

        // Add missing CP users into revalUserReportRecords
        addDataFromCpRolesRecordToUserReportRecord(cpUserRecords, revalCpUserRolesRecords,
                revalUserReportRecords, revalUserHeaderRecord);

        logger.info("Total Count of user record-> {}", revalUserReportRecords.size());
        return revalUserReportRecords;

    } catch (IOException e) {
        logger.error("[Reval - Process] error processing Daily REVal SIAM sync report : {}", e.getMessage());
        throw new InternalServerException("Error Processing Daily Reval SIAM Sync");
    }
}

//---
private void addDataFromCPRolesRecordToUserReportRecord(
        List<CSVRecord> cpUserRecords,
        List<CSVRecord> revalCpUserRolesRecords,
        List<CSVRecord> revalUserReportRecords,
        CSVRecord revalUserHeaderRecord) {

    Map<String, CSVRecord> cpUserRoleMap = revalCpUserRolesRecords.stream()
            .collect(Collectors.toMap(r -> r.get("User"), r -> r, (r1, r2) -> r1));

    Set<String> existingNames = revalUserReportRecords.stream()
            .map(r -> r.get(RevalServiceConstants.FIRSTNAME) + "|" + r.get(RevalServiceConstants.LASTNAME))
            .collect(Collectors.toSet());

    // Use index-based loop instead of AtomicInteger
    for (int i = 1; i < cpUserRecords.size(); i++) { // skip first record
        CSVRecord record = cpUserRecords.get(i);
        String userId = record.get("User");
        CSVRecord cpUserRoleRecord = cpUserRoleMap.get(userId);
        if (cpUserRoleRecord == null) continue;

        String[] userNameArray = cpUserRoleRecord.get(RevalServiceConstants.USERNAME1).split("-");
        String nameKey = userNameArray[0] + "|" + userNameArray[1];

        boolean alreadyExists = existingNames.contains(nameKey);
        boolean isDcpUser = RevalServiceConstants.getDCPUserMap().containsValue(userId);

        if (!alreadyExists && !isDcpUser) {
            List<String> newRecordValues = createNewRecord(cpUserRoleRecord, revalUserHeaderRecord, userNameArray);

            // Build one CSV line
            String joinedLine = String.join(",", newRecordValues);

            try (CSVParser parser = CSVParser.parse(joinedLine,
                    CSVFormat.DEFAULT.withHeader(revalUserHeaderRecord.toMap().keySet().toArray(new String[0])))) {
                CSVRecord newUserRecord = parser.getRecords().get(0);
                revalUserReportRecords.add(newUserRecord);
                existingNames.add(nameKey);
            } catch (IOException e) {
                logger.error("Error creating new CSVRecord: {}", e.getMessage());
            }
        }
    }
}





////////////

private void addDataFromCPRolesRecordToUserReportRecord(
        List<CSVRecord> cpUserRecords,
        List<CSVRecord> revalCpUserRolesRecords,
        List<CSVRecord> revalUserReportRecords,
        CSVRecord revalUserHeaderRecord) {

    // Pre-index CP user roles by "User"
    Map<String, CSVRecord> cpUserRoleMap = revalCpUserRolesRecords.stream()
            .collect(Collectors.toMap(r -> r.get("User"), r -> r, (r1, r2) -> r1));

    // Pre-index existing reval users by FirstName+LastName for quick lookup
    Set<String> existingNames = revalUserReportRecords.stream()
            .map(r -> r.get(RevalServiceConstants.FIRSTNAME) + "|" + r.get(RevalServiceConstants.LASTNAME))
            .collect(Collectors.toSet());

    AtomicInteger counter = new AtomicInteger(0);

    for (CSVRecord record : cpUserRecords) {
        if (counter.get() > 0) {
            String userId = record.get("User");
            CSVRecord cpUserRoleRecord = cpUserRoleMap.get(userId);
            if (cpUserRoleRecord == null) {
                counter.incrementAndGet();
                continue;
            }

            String[] userNameArray = cpUserRoleRecord.get(RevalServiceConstants.USERNAME1).split("-");
            String nameKey = userNameArray[0] + "|" + userNameArray[1];

            boolean alreadyExists = existingNames.contains(nameKey);
            boolean isDcpUser = RevalServiceConstants.getDCPUserMap().containsValue(userId);

            if (!alreadyExists && !isDcpUser) {
                List<String> newRecordValues = createNewRecord(cpUserRoleRecord, revalUserHeaderRecord, userNameArray);

                // Instead of building a CSV string and reparsing, join values directly
                String joinedLine = String.join(",", newRecordValues);

                try (CSVParser parser = CSVParser.parse(joinedLine,
                        CSVFormat.DEFAULT.withHeader(revalUserHeaderRecord.toMap().keySet().toArray(new String[0])))) {
                    CSVRecord newUserRecord = parser.getRecords().get(0);
                    revalUserReportRecords.add(newUserRecord);
                    existingNames.add(nameKey);
                } catch (IOException e) {
                    logger.error("Error creating new CSVRecord: {}", e.getMessage());
                }
            }
        }
        counter.incrementAndGet();
    }
}



//////////////////////

private void populateRoles(List<CSVRecord> rolesRecords, RevalUser revalUser, String roleType, boolean dailyRun) {
    if (roleType.equals(RevalServiceConstants.REVAL)) {
        // Pre-index roles by USERNAME2
        Map<String, List<CSVRecord>> rolesByUser = rolesRecords.stream()
                .collect(Collectors.groupingBy(r -> r.get(RevalServiceConstants.USERNAME2)));

        List<CSVRecord> filteredRecords = rolesByUser.getOrDefault(revalUser.getUserName(), Collections.emptyList());

        List<UserRole> roleList = new ArrayList<>();
        for (CSVRecord record : filteredRecords) {
            UserRole role = new UserRole();
            role.setRoleName(record.get("Role"));
            role.setRoleType("RevalRole");
            roleList.add(role);
        }
        revalUser.setUserRoleList(roleList);

    } else if (roleType.equals(RevalServiceConstants.CP)) {
        // Pre-index roles by "User"
        Map<String, List<CSVRecord>> rolesByUser = rolesRecords.stream()
                .collect(Collectors.groupingBy(r -> r.get("User")));

        List<CSVRecord> filteredRecords = rolesByUser.getOrDefault(revalUser.getUserName(), Collections.emptyList());

        if (filteredRecords.isEmpty() && RevalServiceConstants.getDcpUserMap().containsKey(revalUser.getUserName())) {
            String mappedUser = RevalServiceConstants.getDcpUserMap().get(revalUser.getUserName());
            filteredRecords = rolesByUser.getOrDefault(mappedUser, Collections.emptyList());
        }

        if (filteredRecords.isEmpty()) {
            String userName = revalUser.getFirstName() + " " + revalUser.getLastName();
            filteredRecords = rolesByUser.getOrDefault(userName, Collections.emptyList());
        }

        for (CSVRecord record : filteredRecords) {
            UserRole role = new UserRole();
            role.setRoleType("Cash&Payment Profile");
            role.setRoleName(dailyRun ? record.get("User Profile Name") : record.get("Function Profile Name"));
            revalUser.getUserRoleList().add(role);
        }
    }
}
