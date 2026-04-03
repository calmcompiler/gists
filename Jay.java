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
