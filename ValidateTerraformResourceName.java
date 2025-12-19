/**
 * Validates whether a given string is a valid Terraform resource name.
 *
 * <p>According to HashiCorp Terraform naming conventions:
 * <ul>
 *   <li>Allowed characters: letters (A-Z, a-z), digits (0-9),
 *       spaces, underscores (_), and dashes (-).</li>
 *   <li>First character: must be a letter (A-Z or a-z) or underscore (_).</li>
 *   <li>Special characters (e.g., @, ., :, ?, /) are not permitted.</li>
 * </ul>
 *
 * <p>The regex used is:
 * <pre>
 * ^[A-Za-z_][A-Za-z0-9 _-]*$
 * </pre>
 *
 * @param productName the resource name to validate
 */
public void validateTerraformResourceName(String productName) {
    boolean isValidTFResourceName = StringUtils.isNotBlank(productName) && productName.matches("^[A-Za-z_][A-Za-z0-9 _-]*$);
    if(!isValidTFResourceName) {
        throw new CatalogException("Invalid terraform resource name: must start with a letter or underscore and may contain only letters, digits, spaces, underscores (_), and dashes (-). [productName=" + productName + "]");
    }
}
