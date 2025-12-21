# String resources reorganization script
# This script reorganizes Korean, Japanese, and Indonesian string files to match English structure

param(
    [string]$EnglishPath = "G:\Workspace\AlcoholicTimer\app\src\main\res\values\strings.xml",
    [string]$TargetLangPath,
    [string]$OutputPath
)

# Load XML files
[xml]$englishXml = Get-Content $EnglishPath -Encoding UTF8
[xml]$targetXml = Get-Content $TargetLangPath -Encoding UTF8

# Create hashtable for target language strings
$targetStrings = @{}
foreach ($string in $targetXml.resources.string) {
    if ($string.name) {
        $targetStrings[$string.name] = $string
    }
}

# Create hashtable for target language string-arrays
$targetArrays = @{}
foreach ($array in $targetXml.resources.'string-array') {
    if ($array.name) {
        $targetArrays[$array.name] = $array
    }
}

# Create new XML with UTF-8 encoding
$xmlWriter = New-Object System.Xml.XmlTextWriter($OutputPath, [System.Text.Encoding]::UTF8)
$xmlWriter.Formatting = 'Indented'
$xmlWriter.Indentation = 4

# Write XML declaration
$xmlWriter.WriteStartDocument()
$xmlWriter.WriteStartElement('resources')

# Track current comment
$currentComment = $null

# Process English XML to get structure
$englishContent = Get-Content $EnglishPath -Raw -Encoding UTF8
$lines = $englishContent -split "`n"

foreach ($line in $lines) {
    $trimmedLine = $line.Trim()

    # Skip XML declaration and resources tags
    if ($trimmedLine -match '^\<\?xml' -or $trimmedLine -eq '<resources>' -or $trimmedLine -eq '</resources>') {
        continue
    }

    # Handle comments
    if ($trimmedLine -match '^\<\!--(.+)--\>$') {
        $commentText = $matches[1].Trim()
        $xmlWriter.WriteComment(" $commentText ")
        continue
    }

    # Handle string elements
    if ($trimmedLine -match '^\<string name="([^"]+)"') {
        $stringName = $matches[1]
        if ($targetStrings.ContainsKey($stringName)) {
            $targetString = $targetStrings[$stringName]

            # Write string element
            $xmlWriter.WriteStartElement('string')
            $xmlWriter.WriteAttributeString('name', $stringName)

            # Copy other attributes if present
            foreach ($attr in $targetString.Attributes) {
                if ($attr.Name -ne 'name') {
                    $xmlWriter.WriteAttributeString($attr.Name, $attr.Value)
                }
            }

            # Write inner XML (preserves special characters)
            $xmlWriter.WriteRaw($targetString.InnerXml)
            $xmlWriter.WriteEndElement()
        }
        continue
    }

    # Handle string-array elements
    if ($trimmedLine -match '^\<string-array name="([^"]+)"') {
        $arrayName = $matches[1]
        if ($targetArrays.ContainsKey($arrayName)) {
            $targetArray = $targetArrays[$arrayName]

            # Write entire array with items
            $xmlWriter.WriteRaw("`n    " + $targetArray.OuterXml.Replace("`r`n", "`n    ") + "`n    ")
        }
        continue
    }

    # Handle blank lines (preserve spacing)
    if ($trimmedLine -eq '') {
        # Don't write extra blank lines
        continue
    }
}

# Close root element
$xmlWriter.WriteEndElement()
$xmlWriter.WriteEndDocument()
$xmlWriter.Close()

Write-Host "Successfully reorganized: $OutputPath"

