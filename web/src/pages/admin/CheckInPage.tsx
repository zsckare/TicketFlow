import {
  useEffect,
  useRef,
  useState,
} from 'react'

import {
  useMutation,
} from '@tanstack/react-query'

import {
  Html5Qrcode,
} from 'html5-qrcode'

import {
  apiRequest,
  json,
} from '../../api/httpClient'

import {
  ErrorState,
} from '../../components/Ui'

import type {
  IssuedTicketResponse,
} from '../../types/ticketsOwned'

const SCANNER_ELEMENT_ID =
  'ticketflow-qr-reader'

export function CheckInPage() {
  const [payload, setPayload] =
    useState('')

  const [scanning, setScanning] =
    useState(false)

  const [scanError, setScanError] =
    useState('')

  /*
   * Conservamos la instancia del scanner fuera del estado
   * de React porque Html5Qrcode mantiene internamente
   * recursos asociados a la cámara.
   */
  const scanner =
    useRef<Html5Qrcode | null>(null)

  /*
   * Evita que varios frames del mismo QR produzcan
   * múltiples peticiones de check-in.
   */
  const scanLocked =
    useRef(false)

  const stop = async () => {
    const current =
      scanner.current

    scanner.current = null
    scanLocked.current = false

    if (current) {
      try {
        if (current.isScanning) {
          await current.stop()
        }
      } catch {
        // El scanner puede haberse detenido previamente.
      }

      try {
        current.clear()
      } catch {
        // No necesitamos bloquear la UI si clear falla.
      }
    }

    setScanning(false)
  }

  const checkIn =
    useMutation({
      mutationFn:
        (qrPayload: string) =>
          apiRequest<IssuedTicketResponse>(
            '/orders/tickets/check-in',
            json({
              qrPayload,
            }),
          ),

      onSuccess: () => {
        setPayload('')
        void stop()
      },

      onError: () => {
        /*
         * Permitimos volver a escanear después de un
         * QR inválido o un boleto ya utilizado.
         */
        scanLocked.current = false
      },
    })

  const submitPayload = (
    value: string,
  ) => {
    const normalized =
      value.trim()

    if (
      !normalized ||
      checkIn.isPending
    ) {
      return
    }

    checkIn.mutate(normalized)
  }

  const start = async () => {
    setScanError('')
    checkIn.reset()

    if (
      !navigator.mediaDevices ||
      !navigator.mediaDevices.getUserMedia
    ) {
      setScanError(
        'La cámara no está disponible. Abre TicketFlow mediante HTTPS o utiliza la validación manual.',
      )

      return
    }

    try {
      /*
       * Esperamos a que React renderice el contenedor
       * antes de entregar su ID a Html5Qrcode.
       */
      setScanning(true)

      await new Promise<void>(
        resolve => {
          requestAnimationFrame(
            () => resolve(),
          )
        },
      )

      const instance =
        new Html5Qrcode(
          SCANNER_ELEMENT_ID,
        )

      scanner.current =
        instance

      await instance.start(
        {
          facingMode:
            'environment',
        },
        {
          fps: 10,

          /*
           * Área central utilizada para detectar el QR.
           * El valor es suficientemente grande para
           * teléfonos sin ocupar toda la cámara.
           */
          qrbox: {
            width: 250,
            height: 250,
          },
        },

        decodedText => {
          const qrPayload =
            decodedText.trim()

          if (
            !qrPayload ||
            scanLocked.current
          ) {
            return
          }

          scanLocked.current = true

          submitPayload(
            qrPayload,
          )
        },

        () => {
          /*
           * Html5Qrcode llama este callback continuamente
           * cuando un frame no contiene un QR.
           *
           * No es un error para el usuario.
           */
        },
      )
    } catch (error) {
      await stop()

      setScanError(
        error instanceof Error
          ? error.message
          : 'No fue posible abrir la cámara',
      )
    }
  }

  useEffect(
    () => {
      return () => {
        void stop()
      }
    },
    [],
  )

  return (
    <section className="checkin-layout">
      <div className="panel checkin-card">
        <div className="checkin-icon">
          ⌁
        </div>

        <span className="eyebrow">
          Control de acceso
        </span>

        <h2>
          Escanear boleto
        </h2>

        <p className="muted">
          Un solo escaneo valida la firma
          del QR y marca el boleto como
          utilizado de forma atómica.
        </p>

        {scanning ? (
          <div className="scanner">
            <div
              id={
                SCANNER_ELEMENT_ID
              }
              className="qr-reader"
            />

            <button
              className="button secondary full"
              onClick={() => {
                void stop()
              }}
            >
              Cerrar cámara
            </button>
          </div>
        ) : (
          <button
            className="button primary full button-lg"
            onClick={() => {
              void start()
            }}
          >
            Abrir scanner QR
          </button>
        )}

        <div className="manual-checkin">
          <span>
            o pega el payload firmado
          </span>

          <div className="stack">
            <input
              className="token-input"
              value={payload}
              onChange={
                event =>
                  setPayload(
                    event.target.value,
                  )
              }
              placeholder="ticketflow:v1:..."
            />

            <button
              className="button secondary full"
              disabled={
                !payload.trim() ||
                checkIn.isPending
              }
              onClick={() =>
                submitPayload(
                  payload,
                )
              }
            >
              {checkIn.isPending
                ? 'Validando…'
                : 'Validar entrada'}
            </button>
          </div>
        </div>

        {scanError && (
          <div className="alert info">
            {scanError}
          </div>
        )}

        {checkIn.data && (
          <div className="alert success">
            <b>
              ✓ Entrada válida
            </b>

            <span>
              {checkIn.data
                .eventName ??
                'Boleto'}

              {checkIn.data
                .seatLabel
                ? ` · Asiento ${checkIn.data.seatLabel}`
                : ''}
            </span>
          </div>
        )}

        {checkIn.error && (
          <ErrorState
            error={
              checkIn.error
            }
          />
        )}
      </div>

      <aside className="panel checkin-help">
        <h3>
          Check-in de un paso
        </h3>

        <ol>
          <li>
            Abre el scanner.
          </li>

          <li>
            Apunta al QR del
            asistente.
          </li>

          <li>
            TicketFlow valida la
            firma y el estado.
          </li>

          <li>
            Si es válido, queda
            USED inmediatamente.
          </li>
        </ol>

        <p className="muted">
          Un segundo escaneo del
          mismo boleto será
          rechazado.
        </p>
      </aside>
    </section>
  )
}