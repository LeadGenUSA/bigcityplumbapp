import SwiftUI
import UIKit

private let SERVICE_TYPES: [String] = [
    "Emergency leak / burst pipe",
    "Clogged drain or toilet",
    "Water heater problem",
    "Heating system service",
    "Bathroom or kitchen remodel",
    "Inspection / quote",
    "Other",
]

struct ServiceRequestView: View {
    @Environment(\.openURL) private var openURL

    @State private var name = ""
    @State private var phone = ""
    @State private var email = ""
    @State private var address = ""
    @State private var serviceType: String = SERVICE_TYPES.first!
    @State private var details = ""

    @State private var showMail = false
    @State private var alertMessage: String?
    @State private var showSuccess = false

    var body: some View {
        Form {
            Section {
                Button {
                    if let url = AppConfig.telURL { openURL(url) }
                } label: {
                    HStack {
                        Image(systemName: "phone.fill")
                        Text("Call now: \(AppConfig.phoneNumberDisplay)").bold()
                    }
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .foregroundStyle(.white)
                    .background(Theme.brandOrange)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)
            } header: {
                Text("Emergency? Call us first.")
            }

            Section("Your information") {
                TextField("Name", text: $name)
                    .textContentType(.name)
                TextField("Phone", text: $phone)
                    .keyboardType(.phonePad)
                    .textContentType(.telephoneNumber)
                TextField("Email (optional)", text: $email)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                    .textInputAutocapitalization(.never)
                TextField("Service address", text: $address, axis: .vertical)
                    .textContentType(.fullStreetAddress)
                    .lineLimit(2...4)
            }

            Section("Type of service") {
                Picker("Type", selection: $serviceType) {
                    ForEach(SERVICE_TYPES, id: \.self) { Text($0) }
                }
                .pickerStyle(.menu)
            }

            Section("Describe the problem") {
                TextField("Details", text: $details, axis: .vertical)
                    .lineLimit(4...8)
            }

            Section {
                Button(action: submit) {
                    HStack {
                        Image(systemName: "envelope.fill")
                        Text("Submit Request").bold()
                    }
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .foregroundStyle(.white)
                    .background(Theme.brandBlue)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)
            }
        }
        .navigationTitle("Request Service")
        .alert("Missing info", isPresented: Binding(
            get: { alertMessage != nil },
            set: { if !$0 { alertMessage = nil } }
        )) {
            Button("OK") { alertMessage = nil }
        } message: {
            Text(alertMessage ?? "")
        }
        .alert("Request prepared", isPresented: $showSuccess) {
            Button("OK") { showSuccess = false }
        } message: {
            Text("We've opened your email app. Send the message to finish your request.")
        }
    }

    private func submit() {
        if name.isEmpty {
            alertMessage = "Please enter your name."; return
        }
        if phone.isEmpty {
            alertMessage = "Please enter a phone number we can reach you at."; return
        }
        if address.isEmpty {
            alertMessage = "Please enter the service address."; return
        }

        let subject = "Service request: \(serviceType)"
        var bodyLines: [String] = [
            "Name: \(name)",
            "Phone: \(phone)",
        ]
        if !email.isEmpty { bodyLines.append("Email: \(email)") }
        bodyLines.append(contentsOf: [
            "Address: \(address)",
            "Service: \(serviceType)",
            "",
            "Details:",
            details.isEmpty ? "(none)" : details,
        ])
        let body = bodyLines.joined(separator: "\n")

        let allowed = CharacterSet.urlQueryAllowed
        guard
            let subjEnc = subject.addingPercentEncoding(withAllowedCharacters: allowed),
            let bodyEnc = body.addingPercentEncoding(withAllowedCharacters: allowed),
            let url = URL(string: "mailto:\(AppConfig.email)?subject=\(subjEnc)&body=\(bodyEnc)")
        else {
            alertMessage = "Couldn't build the email. Please call \(AppConfig.phoneNumberDisplay)."
            return
        }

        UIApplication.shared.open(url) { ok in
            if ok {
                showSuccess = true
            } else {
                alertMessage = "No email app found. Please call \(AppConfig.phoneNumberDisplay)."
            }
        }
    }
}

#Preview {
    NavigationStack { ServiceRequestView() }
}
