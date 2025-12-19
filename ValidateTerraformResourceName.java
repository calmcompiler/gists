/**
 * Validates whether a given string is a valid Terraform resource name.
 *
 * <p>According to HashiCorp Terraform naming conventions:
 * <ul>
 *   <li>Allowed characters: letters (<code>A-Z</code>, <code>a-z</code>), digits (<code>0-9</code>),
 *       underscores (<code>_</code>), and dashes (<code>-</code>).</li>
 *   <li>First character: must be a letter (<code>A-Z</code> or <code>a-z</code>) or underscore (<code>_</code>).</li>
 *   <li>Spaces and special characters (e.g., <code>@</code>, <code>.</code>, <code>:</code>, <code>?</code>) are not permitted.</li>
 * </ul>
 *
 * <p>The regex used is:
 * <pre>
 * ^[A-Za-z_][A-Za-z0-9_-]*$
 * </pre>
 *
 * <p>Examples:
 * <ul>
 *   <li><code>my_resource</code> → valid</li>
 *   <li><code>MyResource</code> → valid</li>
 *   <li><code>resource-123</code> → valid</li>
 *   <li><code>_Resource</code> → valid</li>
 *   <li><code>123resource</code> → invalid (starts with digit)</li>
 *   <li><code>res@name</code> → invalid (special character not allowed)</li>
 * </ul>
 *
 * @param productName the resource name to validate
 * @return {@code true} if the name is valid according to Terraform conventions, {@code false} otherwise
 */
public boolean isValidTerraformResourceName(String productName) {
    if (productName == null) {
        return false;
    }
    return productName.matches("^[A-Za-z_][A-Za-z0-9_-]*$");
}



public static final String TERRAFORM_NAME_ERROR =
    "Invalid Terraform resource name. " +
    "It must start with a letter or underscore and may contain only letters, digits, underscores (_), and dashes (-).";
