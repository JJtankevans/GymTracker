// Lê o texto de uma captura de tela (Vision) e imprime uma linha por trecho encontrado, mais quanto da tela
// não está preto. Usado pelo CI de iOS para conferir, sem ver a imagem, que o app abriu na tela certa.
import Foundation
import Vision
import AppKit

let path = CommandLine.arguments[1]
guard let image = NSImage(contentsOfFile: path),
      let cg = image.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
    print("(não abriu a imagem)"); exit(0)
}

// quanto da tela tem conteúdo (pixel com brilho > 40 de 255), numa amostra de 1 a cada 8 pixels
let w = cg.width, h = cg.height
var lit = 0, total = 0
if let data = cg.dataProvider?.data, let ptr = CFDataGetBytePtr(data) {
    let bpp = cg.bitsPerPixel / 8, row = cg.bytesPerRow
    var y = 0
    while y < h {
        var x = 0
        while x < w {
            let i = y * row + x * bpp
            let lum = (Int(ptr[i]) + Int(ptr[i + 1]) + Int(ptr[i + 2])) / 3
            if lum > 40 { lit += 1 }
            total += 1
            x += 8
        }
        y += 8
    }
}
print("tela \(w)x\(h), com conteúdo: \(total > 0 ? lit * 100 / total : 0)%")

let request = VNRecognizeTextRequest { req, _ in
    for case let obs as VNRecognizedTextObservation in req.results ?? [] {
        if let top = obs.topCandidates(1).first { print(top.string) }
    }
}
request.recognitionLevel = .accurate
request.usesCPUOnly = true          // a máquina virtual do CI não tem o acelerador de imagem
request.recognitionLanguages = ["pt-BR", "en-US"]
do {
    try VNImageRequestHandler(cgImage: cg).perform([request])
} catch {
    print("(OCR falhou: \(error))")
}
