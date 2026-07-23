param(
    [string]$Src = "icon.png",
    [string]$Out = "icon.ico"
)
Add-Type -AssemblyName System.Drawing

$srcPath = (Resolve-Path $Src).Path
$source = [System.Drawing.Image]::FromFile($srcPath)

$sizes = @(16, 32, 48, 64, 256)
$pngBlobs = @()

foreach ($s in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap($s, $s)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.DrawImage($source, 0, 0, $s, $s)
    $g.Dispose()

    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $pngBlobs += ,($ms.ToArray())
    $ms.Dispose()
}
$source.Dispose()

$count = $pngBlobs.Count
$fs = New-Object System.IO.FileStream((Join-Path (Get-Location) $Out), [System.IO.FileMode]::Create)
$bw = New-Object System.IO.BinaryWriter($fs)

# ICONDIR
$bw.Write([UInt16]0)      # reserved
$bw.Write([UInt16]1)      # type = icon
$bw.Write([UInt16]$count) # image count

# offset where image data begins: 6 (ICONDIR) + 16 per entry
$offset = 6 + (16 * $count)

for ($i = 0; $i -lt $count; $i++) {
    $s = $sizes[$i]
    $data = $pngBlobs[$i]
    $wByte = if ($s -ge 256) { 0 } else { $s }
    $hByte = if ($s -ge 256) { 0 } else { $s }
    $bw.Write([Byte]$wByte)  # width (0 = 256)
    $bw.Write([Byte]$hByte)  # height (0 = 256)
    $bw.Write([Byte]0)       # color count
    $bw.Write([Byte]0)       # reserved
    $bw.Write([UInt16]1)     # color planes
    $bw.Write([UInt16]32)    # bits per pixel
    $bw.Write([UInt32]$data.Length) # size of image data
    $bw.Write([UInt32]$offset)      # offset of image data
    $offset += $data.Length
}

foreach ($data in $pngBlobs) {
    $bw.Write($data)
}

$bw.Flush()
$bw.Close()
$fs.Close()

Write-Output "ICO ecrit: $Out ($count images: $($sizes -join ','))"
