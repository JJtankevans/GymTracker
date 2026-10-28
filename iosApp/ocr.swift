// Lê o texto de uma captura de tela (Vision) e imprime uma linha por trecho encontrado.
// Usado pelo CI de iOS para conferir, sem ver a imagem, que o app abriu na tela certa.
import Foundation
import Vision
import AppKit

let path = CommandLine.arguments[1]
guard let image = NSImage(contentsOfFile: path),
      let cg = image.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
    print("(não abriu a imagem)"); exit(0)
}
let request = VNRecognizeTextRequest { req, _ in
    for case let obs as VNRecognizedTextObservation in req.results ?? [] {
        if let top = obs.topCandidates(1).first { print(top.string) }
    }
}
request.recognitionLevel = .accurate
request.recognitionLanguages = ["pt-BR", "en-US"]
try? VNImageRequestHandler(cgImage: cg).perform([request])
